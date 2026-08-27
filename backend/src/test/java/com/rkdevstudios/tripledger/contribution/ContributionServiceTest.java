package com.rkdevstudios.tripledger.contribution;

import com.rkdevstudios.tripledger.contribution.application.ContributionService;
import com.rkdevstudios.tripledger.contribution.domain.*;
import com.rkdevstudios.tripledger.workspace.domain.MemberRole;
import com.rkdevstudios.tripledger.workspace.domain.Workspace;
import com.rkdevstudios.tripledger.workspace.domain.WorkspaceRepository;
import com.rkdevstudios.tripledger.workspace.domain.WorkspaceMember;
import com.rkdevstudios.tripledger.workspace.domain.WorkspaceMemberRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;

import com.rkdevstudios.tripledger.expense.application.ContributionExpenseListener;
import com.rkdevstudios.tripledger.expense.domain.*;
import com.rkdevstudios.tripledger.identity.domain.User;
import com.rkdevstudios.tripledger.identity.domain.UserRepository;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class ContributionServiceTest {

    private PlannedContributionRepository plannedContributionRepository;
    private ContributionEntryRepository contributionEntryRepository;
    private WorkspaceMemberRepository workspaceMemberRepository;
    private WorkspaceRepository workspaceRepository;
    private UserRepository userRepository;
    private com.rkdevstudios.tripledger.expense.domain.SplitAllocationRepository splitAllocationRepository;
    private com.rkdevstudios.tripledger.expense.domain.ExpenseRepository expenseRepository;
    private ContributionService contributionService;

    @BeforeEach
    void setUp() {
        plannedContributionRepository = mock(PlannedContributionRepository.class);
        contributionEntryRepository = mock(ContributionEntryRepository.class);
        workspaceMemberRepository = mock(WorkspaceMemberRepository.class);
        workspaceRepository = mock(WorkspaceRepository.class);
        userRepository = mock(UserRepository.class);
        splitAllocationRepository = mock(com.rkdevstudios.tripledger.expense.domain.SplitAllocationRepository.class);
        expenseRepository = mock(com.rkdevstudios.tripledger.expense.domain.ExpenseRepository.class);
        contributionService = new ContributionService(
                plannedContributionRepository,
                contributionEntryRepository,
                workspaceMemberRepository,
                workspaceRepository,
                userRepository,
                splitAllocationRepository,
                expenseRepository
        );
    }

    @Test
    void testEqualSplit() {
        String workspaceId = "ws_1";
        BigDecimal budget = BigDecimal.valueOf(50000);
        List<String> memberIds = Arrays.asList("usr_1", "usr_2", "usr_3", "usr_4", "usr_5");

        Workspace workspace = new Workspace(workspaceId, "Trip", "Desc", LocalDate.now(), LocalDate.now().plusDays(5), "INR", budget, 5, "usr_1");
        when(workspaceRepository.findById(workspaceId)).thenReturn(Optional.of(workspace));

        when(plannedContributionRepository.findByWorkspaceIdAndUserId(anyString(), anyString()))
                .thenReturn(Optional.empty());

        contributionService.initializeContributions(
                workspaceId,
                ContributionStrategy.EQUAL,
                budget,
                memberIds,
                null,
                null
        );

        ArgumentCaptor<PlannedContribution> captor = ArgumentCaptor.forClass(PlannedContribution.class);
        verify(plannedContributionRepository, times(5)).save(captor.capture());

        List<PlannedContribution> saved = captor.getAllValues();
        assertEquals(5, saved.size());
        for (PlannedContribution pc : saved) {
            assertEquals(0, pc.getPlannedAmount().compareTo(BigDecimal.valueOf(10000)));
        }
    }

    @Test
    void testCashContribution_StatusTransition() {
        String workspaceId = "ws_1";
        String userId = "usr_1";
        BigDecimal plannedAmount = BigDecimal.valueOf(10000);

        WorkspaceMember member = new WorkspaceMember(workspaceId, "caller_1", MemberRole.ADMIN);
        when(workspaceMemberRepository.findByWorkspaceIdAndUserId(workspaceId, "caller_1"))
                .thenReturn(Optional.of(member));

        PlannedContribution planned = new PlannedContribution("pc_1", workspaceId, userId, plannedAmount);
        when(plannedContributionRepository.findByWorkspaceIdAndUserId(workspaceId, userId))
                .thenReturn(Optional.of(planned));

        List<ContributionEntry> entries = Arrays.asList(
                new ContributionEntry("e_1", workspaceId, userId, ContributionEntryType.CASH, BigDecimal.valueOf(8000), "Cash contribution", null)
        );
        when(contributionEntryRepository.findByWorkspaceIdAndUserId(workspaceId, userId))
                .thenReturn(entries);

        ContributionSummary summary = contributionService.getContributionSummary(workspaceId, userId);

        assertEquals(0, summary.plannedContribution().compareTo(plannedAmount));
        assertEquals(0, summary.cashContributed().compareTo(BigDecimal.valueOf(8000)));
        assertEquals(0, summary.totalContribution().compareTo(BigDecimal.valueOf(8000)));
        assertEquals(0, summary.remainingContribution().compareTo(BigDecimal.valueOf(2000)));
        assertEquals(ContributionStatus.PARTIALLY_FUNDED, summary.status());
    }

    @Test
    void testFullContribution_StatusBecomesFullyFunded() {
        String workspaceId = "ws_1";
        String userId = "usr_1";
        BigDecimal plannedAmount = BigDecimal.valueOf(10000);

        PlannedContribution planned = new PlannedContribution("pc_1", workspaceId, userId, plannedAmount);
        when(plannedContributionRepository.findByWorkspaceIdAndUserId(workspaceId, userId))
                .thenReturn(Optional.of(planned));

        List<ContributionEntry> entries = Arrays.asList(
                new ContributionEntry("e_1", workspaceId, userId, ContributionEntryType.CASH, BigDecimal.valueOf(10000), "Cash contribution", null)
        );
        when(contributionEntryRepository.findByWorkspaceIdAndUserId(workspaceId, userId))
                .thenReturn(entries);

        ContributionSummary summary = contributionService.getContributionSummary(workspaceId, userId);

        assertEquals(ContributionStatus.FULLY_FUNDED, summary.status());
        assertEquals(0, summary.remainingContribution().compareTo(BigDecimal.ZERO));
    }

    @Test
    void testOverContribution_StatusBecomesOverFunded() {
        String workspaceId = "ws_1";
        String userId = "usr_1";
        BigDecimal plannedAmount = BigDecimal.valueOf(10000);

        PlannedContribution planned = new PlannedContribution("pc_1", workspaceId, userId, plannedAmount);
        when(plannedContributionRepository.findByWorkspaceIdAndUserId(workspaceId, userId))
                .thenReturn(Optional.of(planned));

        List<ContributionEntry> entries = Arrays.asList(
                new ContributionEntry("e_1", workspaceId, userId, ContributionEntryType.CASH, BigDecimal.valueOf(12000), "Cash contribution", null)
        );
        when(contributionEntryRepository.findByWorkspaceIdAndUserId(workspaceId, userId))
                .thenReturn(entries);

        ContributionSummary summary = contributionService.getContributionSummary(workspaceId, userId);

        assertEquals(ContributionStatus.OVER_FUNDED, summary.status());
        assertEquals(0, summary.remainingContribution().compareTo(BigDecimal.ZERO));
    }

    @Test
    void testAdjustment_CorrectsPreviousEntry() {
        String workspaceId = "ws_1";
        String userId = "usr_1";
        BigDecimal plannedAmount = BigDecimal.valueOf(10000);

        PlannedContribution planned = new PlannedContribution("pc_1", workspaceId, userId, plannedAmount);
        when(plannedContributionRepository.findByWorkspaceIdAndUserId(workspaceId, userId))
                .thenReturn(Optional.of(planned));

        List<ContributionEntry> entries = Arrays.asList(
                new ContributionEntry("e_1", workspaceId, userId, ContributionEntryType.CASH, BigDecimal.valueOf(10000), "Cash contribution", null),
                new ContributionEntry("e_2", workspaceId, userId, ContributionEntryType.ADJUSTMENT, BigDecimal.valueOf(-500), "Adjustment correction", null)
        );
        when(contributionEntryRepository.findByWorkspaceIdAndUserId(workspaceId, userId))
                .thenReturn(entries);

        ContributionSummary summary = contributionService.getContributionSummary(workspaceId, userId);

        assertEquals(0, summary.totalContribution().compareTo(BigDecimal.valueOf(9500)));
        assertEquals(ContributionStatus.PARTIALLY_FUNDED, summary.status());
    }

    @Test
    void testFinancialSnapshot_AggregatesCorrectly() {
        String workspaceId = "ws_1";
        BigDecimal budget = BigDecimal.valueOf(50000);

        WorkspaceMember m1 = new WorkspaceMember(workspaceId, "usr_1", MemberRole.ADMIN);
        WorkspaceMember m2 = new WorkspaceMember(workspaceId, "usr_2", MemberRole.MEMBER);
        when(workspaceMemberRepository.findByWorkspaceId(workspaceId)).thenReturn(Arrays.asList(m1, m2));
        when(workspaceMemberRepository.findByWorkspaceIdAndUserId(workspaceId, "usr_1")).thenReturn(Optional.of(m1));

        User user1 = new User("usr_1", "Raj", "raj@test.com", "pass", null);
        User user2 = new User("usr_2", "Rahul", "rahul@test.com", "pass", null);
        when(userRepository.findById("usr_1")).thenReturn(Optional.of(user1));
        when(userRepository.findById("usr_2")).thenReturn(Optional.of(user2));

        PlannedContribution p1 = new PlannedContribution("pc_1", workspaceId, "usr_1", BigDecimal.valueOf(25000));
        PlannedContribution p2 = new PlannedContribution("pc_2", workspaceId, "usr_2", BigDecimal.valueOf(25000));
        when(plannedContributionRepository.findByWorkspaceIdAndUserId(workspaceId, "usr_1")).thenReturn(Optional.of(p1));
        when(plannedContributionRepository.findByWorkspaceIdAndUserId(workspaceId, "usr_2")).thenReturn(Optional.of(p2));

        // usr_1 fully funded, usr_2 not started
        when(contributionEntryRepository.findByWorkspaceIdAndUserId(workspaceId, "usr_1"))
                .thenReturn(Arrays.asList(new ContributionEntry("e_1", workspaceId, "usr_1", ContributionEntryType.CASH, BigDecimal.valueOf(25000), "C", null)));
        when(contributionEntryRepository.findByWorkspaceIdAndUserId(workspaceId, "usr_2"))
                .thenReturn(Collections.emptyList());

        // total workspace entries
        when(contributionEntryRepository.findByWorkspaceId(workspaceId))
                .thenReturn(Arrays.asList(new ContributionEntry("e_1", workspaceId, "usr_1", ContributionEntryType.CASH, BigDecimal.valueOf(25000), "C", null)));

        WorkspaceFinancialSnapshot snapshot = contributionService.getFinancialSnapshot(workspaceId, "usr_1", budget, BigDecimal.ZERO);

        assertEquals(2, snapshot.memberCount());
        assertEquals(1, snapshot.fundedMembers());
        assertEquals(1, snapshot.pendingMembers());
        assertEquals(0, snapshot.overFundedMembers());
        assertEquals(0, snapshot.currentFund().compareTo(BigDecimal.valueOf(25000)));
        assertEquals(0, snapshot.fundingGap().compareTo(BigDecimal.valueOf(25000)));
    }

    @Test
    void testEqualSplit_DeterministicRoundingNonDivisible() {
        String workspaceId = "ws_1";
        BigDecimal budget = BigDecimal.valueOf(100.00); // 100 dollars / rupees
        List<String> memberIds = Arrays.asList("usr_c", "usr_a", "usr_b"); // unsorted

        Workspace workspace = new Workspace(workspaceId, "Trip", "Desc", LocalDate.now(), LocalDate.now().plusDays(5), "INR", budget, 3, "usr_1");
        when(workspaceRepository.findById(workspaceId)).thenReturn(Optional.of(workspace));

        when(plannedContributionRepository.findByWorkspaceIdAndUserId(anyString(), anyString()))
                .thenReturn(Optional.empty());

        contributionService.initializeContributions(
                workspaceId,
                ContributionStrategy.EQUAL,
                budget,
                memberIds,
                null,
                null
        );

        ArgumentCaptor<PlannedContribution> captor = ArgumentCaptor.forClass(PlannedContribution.class);
        verify(plannedContributionRepository, times(3)).save(captor.capture());

        List<PlannedContribution> saved = captor.getAllValues();
        assertEquals(3, saved.size());

        // Base share = 100 / 3 = 33.3333. Remainder = 100 - (33.3333 * 3) = 0.0001
        // Lexicographical sorting order: usr_a, usr_b, usr_c
        // usr_a gets remainder (33.3334). usr_b and usr_c get base (33.3333).
        BigDecimal sum = BigDecimal.ZERO;
        for (PlannedContribution pc : saved) {
            sum = sum.add(pc.getPlannedAmount());
            if (pc.getUserId().equals("usr_a")) {
                assertEquals(0, pc.getPlannedAmount().compareTo(BigDecimal.valueOf(33.3334)));
            } else {
                assertEquals(0, pc.getPlannedAmount().compareTo(BigDecimal.valueOf(33.3333)));
            }
        }
        assertEquals(0, sum.compareTo(budget));
    }

    @Test
    void testPlannedContributionsRecalculation_Immutability() {
        String workspaceId = "ws_1";
        BigDecimal initialBudget = BigDecimal.valueOf(35000);
        BigDecimal newBudget = BigDecimal.valueOf(35000);
        List<String> memberIds = Arrays.asList("usr_a", "usr_b", "usr_c", "usr_d", "usr_e");

        Workspace workspace = new Workspace(workspaceId, "Trip", "Desc", LocalDate.now(), LocalDate.now().plusDays(5), "INR", initialBudget, 5, "usr_1");
        when(workspaceRepository.findById(workspaceId)).thenReturn(Optional.of(workspace));

        // Mock existing planned contributions
        PlannedContribution pa = new PlannedContribution("pc_a", workspaceId, "usr_a", BigDecimal.valueOf(7000));
        when(plannedContributionRepository.findByWorkspaceIdAndUserId(workspaceId, "usr_a")).thenReturn(Optional.of(pa));
        when(plannedContributionRepository.findByWorkspaceIdAndUserId(workspaceId, "usr_b")).thenReturn(Optional.empty());
        when(plannedContributionRepository.findByWorkspaceIdAndUserId(workspaceId, "usr_c")).thenReturn(Optional.empty());
        when(plannedContributionRepository.findByWorkspaceIdAndUserId(workspaceId, "usr_d")).thenReturn(Optional.empty());
        when(plannedContributionRepository.findByWorkspaceIdAndUserId(workspaceId, "usr_e")).thenReturn(Optional.empty());

        // usr_a already has actual contribution of 7000
        List<ContributionEntry> entries = Arrays.asList(
                new ContributionEntry("e_1", workspaceId, "usr_a", ContributionEntryType.CASH, BigDecimal.valueOf(7000), "C", null)
        );
        when(contributionEntryRepository.findByWorkspaceIdAndUserId(workspaceId, "usr_a"))
                .thenReturn(entries);

        // Recalculate with planned count = 6
        workspace.setPlannedMemberCount(6);

        contributionService.initializeContributions(
                workspaceId,
                ContributionStrategy.EQUAL,
                newBudget,
                memberIds,
                null,
                null
        );

        // Verify save was triggered with updated planned contribution amounts (e.g. ~5833.3333)
        ArgumentCaptor<PlannedContribution> captor = ArgumentCaptor.forClass(PlannedContribution.class);
        verify(plannedContributionRepository, atLeastOnce()).save(captor.capture());

        // Verify actual contribution entry list has not been modified/deleted
        verify(contributionEntryRepository, never()).save(any());
    }

    @Test
    void testGetContributionSummary_IndividualMode_CalculatesRemainingFromSplit() {
        String workspaceId = "ws_ind";
        String userId = "usr_1";

        Workspace workspace = new Workspace(workspaceId, "Trip", "Desc", LocalDate.now(), LocalDate.now().plusDays(5), "INR", BigDecimal.valueOf(15000), 3, "usr_1");
        workspace.setContributionMode(com.rkdevstudios.tripledger.workspace.domain.ContributionMode.INDIVIDUAL);
        when(workspaceRepository.findById(workspaceId)).thenReturn(Optional.of(workspace));

        PlannedContribution planned = new PlannedContribution("pc_1", workspaceId, userId, BigDecimal.valueOf(5000));
        when(plannedContributionRepository.findByWorkspaceIdAndUserId(workspaceId, userId)).thenReturn(Optional.of(planned));

        // Mock split allocations consumed by user = 2000
        com.rkdevstudios.tripledger.expense.domain.SplitAllocation split = new com.rkdevstudios.tripledger.expense.domain.SplitAllocation("s1", "exp1", userId, new com.rkdevstudios.tripledger.expense.domain.Money(BigDecimal.valueOf(2000), "INR"), BigDecimal.valueOf(2000));
        when(splitAllocationRepository.findByUserId(userId)).thenReturn(List.of(split));

        com.rkdevstudios.tripledger.expense.domain.Expense mockExpense = new com.rkdevstudios.tripledger.expense.domain.Expense("exp1", workspaceId, userId, new com.rkdevstudios.tripledger.expense.domain.Money(BigDecimal.valueOf(2000), "INR"), "Hotel", "cat1", LocalDate.now());
        when(expenseRepository.findById("exp1")).thenReturn(Optional.of(mockExpense));

        ContributionSummary summary = contributionService.getContributionSummary(workspaceId, userId);

        assertEquals(0, BigDecimal.valueOf(5000).compareTo(summary.plannedContribution()));
        assertEquals(0, BigDecimal.valueOf(2000).compareTo(summary.totalContribution()));
        assertEquals(0, BigDecimal.valueOf(3000).compareTo(summary.remainingContribution()));
    }

    @Test
    void testUpdateMemberPlannedContribution_FailsWhenNewAmountLessThanContributed() {
        String workspaceId = "ws_1";
        String targetUser = "usr_target";
        String callerUser = "usr_owner";

        // Mock caller is ADMIN
        WorkspaceMember adminMember = new WorkspaceMember(workspaceId, callerUser, MemberRole.ADMIN);
        when(workspaceMemberRepository.findByWorkspaceIdAndUserId(workspaceId, callerUser)).thenReturn(Optional.of(adminMember));

        // Mock user has already contributed 4000 cash
        ContributionEntry entry = new ContributionEntry("e1", workspaceId, targetUser, ContributionEntryType.CASH, BigDecimal.valueOf(4000), "Cash", null);
        when(contributionEntryRepository.findByWorkspaceIdAndUserId(workspaceId, targetUser)).thenReturn(List.of(entry));

        // Attempting to reduce planned contribution to 2000 should fail
        assertThrows(IllegalArgumentException.class, () -> {
            contributionService.updateMemberPlannedContribution(workspaceId, targetUser, BigDecimal.valueOf(2000), callerUser);
        });
    }

    @Test
    void testNonAdminUpdateContributionLedger() {
        String workspaceId = "ws_1";
        String payerId = "usr_member";
        BigDecimal originalAmount = BigDecimal.valueOf(500);
        BigDecimal updatedAmount = BigDecimal.valueOf(100);
        String expenseId = "exp_100";

        // Mock users and roles (Payer is a non-admin MEMBER)
        WorkspaceMember member = new WorkspaceMember(workspaceId, payerId, MemberRole.MEMBER);
        when(workspaceMemberRepository.findByWorkspaceIdAndUserId(workspaceId, payerId))
                .thenReturn(Optional.of(member));

        ContributionEntry mockDirectExpense = new ContributionEntry("e1", workspaceId, payerId, ContributionEntryType.DIRECT_EXPENSE, originalAmount, "Direct Expense: beach party", expenseId);
        when(contributionEntryRepository.save(any(ContributionEntry.class))).thenAnswer(i -> i.getArgument(0));

        ContributionExpenseListener listener = new ContributionExpenseListener(contributionService);

        // Original Expense
        Expense oldExpense = new Expense(expenseId, workspaceId, payerId, new Money(originalAmount, "INR"), "beach party", "cat_1", LocalDate.now(), java.time.Instant.now(), null, null, SplitType.EQUAL, payerId);
        // Updated Expense
        Expense updatedExpense = new Expense(expenseId, workspaceId, payerId, new Money(updatedAmount, "INR"), "beach party", "cat_1", LocalDate.now(), java.time.Instant.now(), null, null, SplitType.EQUAL, payerId);

        ExpenseUpdatedEvent event = new ExpenseUpdatedEvent(oldExpense, updatedExpense);

        // Trigger listener - should not throw SecurityException since it bypasses auth via recordAdjustmentInternal
        assertDoesNotThrow(() -> listener.onExpenseUpdated(event));

        // Verify recordAdjustmentInternal saves the negative adjustment and directExpense is recorded for new amount
        verify(contributionEntryRepository, times(1)).save(argThat(entry -> 
            entry.getEntryType() == ContributionEntryType.ADJUSTMENT && 
            entry.getAmount().compareTo(originalAmount.negate()) == 0
        ));

        verify(contributionEntryRepository, times(1)).save(argThat(entry -> 
            entry.getEntryType() == ContributionEntryType.DIRECT_EXPENSE && 
            entry.getAmount().compareTo(updatedAmount) == 0
        ));
    }

    @Test
    void testNonAdminDeleteContributionLedger() {
        String workspaceId = "ws_1";
        String payerId = "usr_member";
        BigDecimal amount = BigDecimal.valueOf(100);
        String expenseId = "exp_100";

        // Payer is a non-admin MEMBER
        WorkspaceMember member = new WorkspaceMember(workspaceId, payerId, MemberRole.MEMBER);
        when(workspaceMemberRepository.findByWorkspaceIdAndUserId(workspaceId, payerId))
                .thenReturn(Optional.of(member));

        when(contributionEntryRepository.save(any(ContributionEntry.class))).thenAnswer(i -> i.getArgument(0));

        ContributionExpenseListener listener = new ContributionExpenseListener(contributionService);

        Expense deletedExpense = new Expense(expenseId, workspaceId, payerId, new Money(amount, "INR"), "beach party", "cat_1", LocalDate.now(), java.time.Instant.now(), null, null, SplitType.EQUAL, payerId);
        ExpenseDeletedEvent event = new ExpenseDeletedEvent(deletedExpense, "reason", payerId);

        // Trigger listener - should not throw SecurityException
        assertDoesNotThrow(() -> listener.onExpenseDeleted(event));

        // Verify recordAdjustmentInternal saves the negative adjustment to zero out contribution
        verify(contributionEntryRepository, times(1)).save(argThat(entry -> 
            entry.getEntryType() == ContributionEntryType.ADJUSTMENT && 
            entry.getAmount().compareTo(amount.negate()) == 0
        ));
    }
}
