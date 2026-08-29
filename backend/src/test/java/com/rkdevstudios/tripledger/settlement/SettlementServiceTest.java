package com.rkdevstudios.tripledger.settlement;

import com.rkdevstudios.tripledger.contribution.domain.*;
import com.rkdevstudios.tripledger.expense.domain.*;
import com.rkdevstudios.tripledger.settlement.application.SettlementContributionListener;
import com.rkdevstudios.tripledger.settlement.application.SettlementService;
import com.rkdevstudios.tripledger.settlement.domain.*;
import com.rkdevstudios.tripledger.workspace.domain.Workspace;
import com.rkdevstudios.tripledger.workspace.domain.WorkspaceRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.context.ApplicationEventPublisher;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

public class SettlementServiceTest {

    private SettlementSessionRepository sessionRepository;
    private SettlementTransactionRepository transactionRepository;
    private WorkspaceRepository workspaceRepository;
    private PlannedContributionRepository plannedContributionRepository;
    private ContributionEntryRepository contributionEntryRepository;
    private ExpenseRepository expenseRepository;
    private SplitAllocationRepository splitAllocationRepository;
    private SettlementEngine settlementEngine;
    private com.rkdevstudios.tripledger.settlement.domain.GreedySettlementOptimizer settlementOptimizer;
    private ApplicationEventPublisher eventPublisher;

    private SettlementService settlementService;

    @BeforeEach
    void setUp() {
        sessionRepository = mock(SettlementSessionRepository.class);
        transactionRepository = mock(SettlementTransactionRepository.class);
        workspaceRepository = mock(WorkspaceRepository.class);
        plannedContributionRepository = mock(PlannedContributionRepository.class);
        contributionEntryRepository = mock(ContributionEntryRepository.class);
        expenseRepository = mock(ExpenseRepository.class);
        splitAllocationRepository = mock(SplitAllocationRepository.class);
        settlementEngine = new SettlementEngine();
        settlementOptimizer = new com.rkdevstudios.tripledger.settlement.domain.GreedySettlementOptimizer();
        eventPublisher = mock(ApplicationEventPublisher.class);

        settlementService = new SettlementService(
                sessionRepository,
                transactionRepository,
                workspaceRepository,
                plannedContributionRepository,
                contributionEntryRepository,
                expenseRepository,
                splitAllocationRepository,
                settlementEngine,
                settlementOptimizer,
                eventPublisher
        );
    }

    @Test
    public void testConfirmSettlementIdempotency() {
        String workspaceId = "ws_1";
        String sessionId = "sess_1";
        String actor = "usr_actor";

        Workspace workspace = new Workspace(workspaceId, "Trip", "Desc", null, null, "INR", BigDecimal.valueOf(1000), 5, actor);
        when(workspaceRepository.findById(workspaceId)).thenReturn(Optional.of(workspace));

        PlannedContribution pc1 = new PlannedContribution("pc1", workspaceId, "usr_1", BigDecimal.valueOf(100));
        PlannedContribution pc2 = new PlannedContribution("pc2", workspaceId, "usr_2", BigDecimal.valueOf(100));
        when(plannedContributionRepository.findByWorkspaceId(workspaceId)).thenReturn(List.of(pc1, pc2));

        // usr_1 paid 100, usr_2 paid 0. Split is 50/50.
        // usr_1 balance is +50, usr_2 balance is -50.
        ContributionEntry ce1 = new ContributionEntry("ce1", workspaceId, "usr_1", ContributionEntryType.DIRECT_EXPENSE, BigDecimal.valueOf(100), "Dinner", "e1");
        when(contributionEntryRepository.findByWorkspaceId(workspaceId)).thenReturn(List.of(ce1));

        SplitAllocation sa1 = new SplitAllocation("sa1", "e1", "usr_1", new Money(BigDecimal.valueOf(50), "INR"), BigDecimal.valueOf(1));
        SplitAllocation sa2 = new SplitAllocation("sa2", "e1", "usr_2", new Money(BigDecimal.valueOf(50), "INR"), BigDecimal.valueOf(1));
        when(splitAllocationRepository.findByExpenseId("e1")).thenReturn(List.of(sa1, sa2));

        Expense exp = new Expense("e1", workspaceId, "usr_1", new Money(BigDecimal.valueOf(100), "INR"), "Dinner", "cat_1", null, null, null, null, SplitType.EQUAL, "usr_1");
        when(expenseRepository.findByWorkspaceIdAndStatusNot(workspaceId, ExpenseStatus.DELETED)).thenReturn(List.of(exp));

        // Setup session state hash matching correctly.
        WorkspaceFinancialState stateBefore = new WorkspaceFinancialState(workspaceId, List.of(pc1, pc2), List.of(ce1), List.of(exp), List.of(sa1, sa2), Collections.emptyList());
        String correctHashBefore = stateBefore.calculateStateHash();

        SettlementSession session = new SettlementSession(sessionId, workspaceId, correctHashBefore, 1);
        when(sessionRepository.findById(sessionId)).thenReturn(Optional.of(session));

        // When we simulate first confirmation, transactionRepository.findByWorkspaceId returns empty
        when(transactionRepository.findByWorkspaceId(workspaceId)).thenReturn(new java.util.ArrayList<>());

        // Compute transferId dynamically matching the optimizer's UUID.nameUUIDFromBytes generation logic
        // GreedySettlementOptimizer outputs debtor as fromUserId, and creditor as toUserId.
        // Debtor is usr_2 (balance -50), Creditor is usr_1 (balance +50). Amount is 50.0000.
        String transferId = UUID.nameUUIDFromBytes(
                ("usr_2" + "usr_1" + "50.0000").getBytes()
        ).toString();

        // Perform first confirmation
        settlementService.confirmSettlement(workspaceId, transferId, sessionId, actor);

        // Verify save was called once
        verify(transactionRepository, times(1)).save(any(SettlementTransaction.class));
        verify(eventPublisher, times(1)).publishEvent(any(SettlementConfirmedEvent.class));

        // Now mock transactionRepository.findByWorkspaceId to return the confirmed transaction for double confirm retry simulation
        SettlementTransaction transaction = new SettlementTransaction("tx_id", workspaceId, sessionId, "usr_2", "usr_1", new Money(BigDecimal.valueOf(50), "INR"));
        transaction.setStatus(SettlementStatus.CONFIRMED);
        
        // Return transaction in repository
        when(transactionRepository.findByWorkspaceId(workspaceId)).thenReturn(List.of(transaction));
        
        // Re-mock session state hash to match state AFTER confirmation (since currentState.calculateStateHash() includes confirmed transaction)
        WorkspaceFinancialState stateAfter = new WorkspaceFinancialState(workspaceId, List.of(pc1, pc2), List.of(ce1), List.of(exp), List.of(sa1, sa2), List.of(transaction));
        String correctHashAfter = stateAfter.calculateStateHash();
        session.setStateHash(correctHashAfter);

        // Retry identical confirmation call
        settlementService.confirmSettlement(workspaceId, transferId, sessionId, actor);

        // Verify save and event publisher calls DID NOT increase (remain at 1)
        verify(transactionRepository, times(1)).save(any(SettlementTransaction.class));
        verify(eventPublisher, times(1)).publishEvent(any(SettlementConfirmedEvent.class));
    }

    @Test
    public void testNonAdminSettlementContributionListenerBypassesAuth() {
        String workspaceId = "ws_1";
        String debtorId = "usr_member";
        String creditorId = "usr_creditor";
        BigDecimal amount = BigDecimal.valueOf(250);

        com.rkdevstudios.tripledger.contribution.domain.ContributionEntryRepository contributionRepo = mock(com.rkdevstudios.tripledger.contribution.domain.ContributionEntryRepository.class);
        com.rkdevstudios.tripledger.workspace.domain.WorkspaceMemberRepository memberRepo = mock(com.rkdevstudios.tripledger.workspace.domain.WorkspaceMemberRepository.class);
        com.rkdevstudios.tripledger.contribution.domain.PlannedContributionRepository plannedRepo = mock(com.rkdevstudios.tripledger.contribution.domain.PlannedContributionRepository.class);
        com.rkdevstudios.tripledger.workspace.domain.WorkspaceRepository wsRepo = mock(com.rkdevstudios.tripledger.workspace.domain.WorkspaceRepository.class);
        com.rkdevstudios.tripledger.identity.domain.UserRepository userRepo = mock(com.rkdevstudios.tripledger.identity.domain.UserRepository.class);
        SplitAllocationRepository saRepo = mock(SplitAllocationRepository.class);
        ExpenseRepository expRepo = mock(ExpenseRepository.class);

        com.rkdevstudios.tripledger.contribution.application.ContributionService contributionService = new com.rkdevstudios.tripledger.contribution.application.ContributionService(
                plannedRepo,
                contributionRepo,
                memberRepo,
                wsRepo,
                userRepo,
                saRepo,
                expRepo
        );

        // Debtor is a non-admin MEMBER
        com.rkdevstudios.tripledger.workspace.domain.WorkspaceMember member = new com.rkdevstudios.tripledger.workspace.domain.WorkspaceMember(workspaceId, debtorId, com.rkdevstudios.tripledger.workspace.domain.MemberRole.MEMBER);
        when(memberRepo.findByWorkspaceIdAndUserId(workspaceId, debtorId)).thenReturn(Optional.of(member));
        when(contributionRepo.save(any(com.rkdevstudios.tripledger.contribution.domain.ContributionEntry.class))).thenAnswer(i -> i.getArgument(0));

        SettlementContributionListener listener = new SettlementContributionListener(contributionService);

        SettlementTransaction st = new SettlementTransaction("st_1", workspaceId, "sess_1", debtorId, creditorId, new Money(amount, "INR"));
        st.setStatus(SettlementStatus.CONFIRMED);

        SettlementConfirmedEvent event = new SettlementConfirmedEvent(st);

        // Should not throw SecurityException even though actor is a non-admin MEMBER
        assertDoesNotThrow(() -> listener.onSettlementConfirmed(event));

        verify(contributionRepo, times(2)).save(any(com.rkdevstudios.tripledger.contribution.domain.ContributionEntry.class));
    }
}
