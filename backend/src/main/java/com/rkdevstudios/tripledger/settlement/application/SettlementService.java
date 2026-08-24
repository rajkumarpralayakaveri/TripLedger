package com.rkdevstudios.tripledger.settlement.application;

import com.rkdevstudios.tripledger.contribution.domain.ContributionEntry;
import com.rkdevstudios.tripledger.contribution.domain.ContributionEntryRepository;
import com.rkdevstudios.tripledger.contribution.domain.PlannedContribution;
import com.rkdevstudios.tripledger.contribution.domain.PlannedContributionRepository;
import com.rkdevstudios.tripledger.expense.domain.*;
import com.rkdevstudios.tripledger.settlement.domain.*;
import com.rkdevstudios.tripledger.workspace.domain.Workspace;
import com.rkdevstudios.tripledger.workspace.domain.WorkspaceRepository;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.rkdevstudios.tripledger.workspace.domain.WorkspaceStatus;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@Transactional
public class SettlementService {

    private final SettlementSessionRepository sessionRepository;
    private final SettlementTransactionRepository transactionRepository;
    private final WorkspaceRepository workspaceRepository;
    private final PlannedContributionRepository plannedContributionRepository;
    private final ContributionEntryRepository contributionEntryRepository;
    private final ExpenseRepository expenseRepository;
    private final SplitAllocationRepository splitAllocationRepository;
    private final SettlementEngine settlementEngine;
    private final SettlementOptimizer settlementOptimizer;
    private final ApplicationEventPublisher eventPublisher;

    public SettlementService(
            SettlementSessionRepository sessionRepository,
            SettlementTransactionRepository transactionRepository,
            WorkspaceRepository workspaceRepository,
            PlannedContributionRepository plannedContributionRepository,
            ContributionEntryRepository contributionEntryRepository,
            ExpenseRepository expenseRepository,
            SplitAllocationRepository splitAllocationRepository,
            SettlementEngine settlementEngine,
            SettlementOptimizer settlementOptimizer,
            ApplicationEventPublisher eventPublisher
    ) {
        this.sessionRepository = sessionRepository;
        this.transactionRepository = transactionRepository;
        this.workspaceRepository = workspaceRepository;
        this.plannedContributionRepository = plannedContributionRepository;
        this.contributionEntryRepository = contributionEntryRepository;
        this.expenseRepository = expenseRepository;
        this.splitAllocationRepository = splitAllocationRepository;
        this.settlementEngine = settlementEngine;
        this.settlementOptimizer = settlementOptimizer;
        this.eventPublisher = eventPublisher;
    }

    private Workspace getWorkspace(String workspaceId) {
        return workspaceRepository.findById(workspaceId)
                .orElseThrow(() -> new IllegalArgumentException("Workspace not found"));
    }

    private WorkspaceFinancialState buildFinancialState(String workspaceId) {
        List<PlannedContribution> pcs = plannedContributionRepository.findByWorkspaceId(workspaceId);
        List<ContributionEntry> ces = contributionEntryRepository.findByWorkspaceId(workspaceId);
        List<Expense> exps = expenseRepository.findByWorkspaceIdAndStatusNot(workspaceId, ExpenseStatus.DELETED);

        // Identify all deleted expenses in this workspace to filter out orphaned ledger entries
        List<Expense> allExpenses = expenseRepository.findByWorkspaceIdAndStatusNot(workspaceId, ExpenseStatus.UNSETTLED); // Querying with a dummy non-deleted status is not possible cleanly, so let's find them or use referenceId matches
        // Actually, we can just find active expense IDs, and if entry is DIRECT_EXPENSE or an ADJUSTMENT that references an expense, we verify the referenced expense is not deleted.
        // Let's get the list of active expense IDs
        java.util.Set<String> activeExpenseIds = exps.stream()
                .map(Expense::getId)
                .collect(Collectors.toSet());

        List<ContributionEntry> activeCes = ces.stream()
                .filter(ce -> {
                    if (ce.getEntryType() == com.rkdevstudios.tripledger.contribution.domain.ContributionEntryType.DIRECT_EXPENSE) {
                        return activeExpenseIds.contains(ce.getReferenceId());
                    }
                    if (ce.getEntryType() == com.rkdevstudios.tripledger.contribution.domain.ContributionEntryType.ADJUSTMENT) {
                        // If the adjustment has a referenceId and it references an expense, check if it's active.
                        // Non-expense adjustments (like repayments or other manual adjustments) have null referenceId and remain included.
                        String refId = ce.getReferenceId();
                        if (refId != null) {
                            return activeExpenseIds.contains(refId);
                        }
                        return true;
                    }
                    return true;
                })
                .collect(Collectors.toList());

        List<SplitAllocation> allocations = new ArrayList<>();
        for (Expense e : exps) {
            allocations.addAll(splitAllocationRepository.findByExpenseId(e.getId()));
        }

        List<SettlementTransaction> confirmed = transactionRepository.findByWorkspaceId(workspaceId).stream()
                .filter(st -> st.getStatus() == SettlementStatus.CONFIRMED)
                .collect(Collectors.toList());

        return new WorkspaceFinancialState(workspaceId, pcs, activeCes, exps, allocations, confirmed);
    }

    public SettlementPlan generatePlan(String workspaceId) {
        Workspace workspace = getWorkspace(workspaceId);
        WorkspaceFinancialState state = buildFinancialState(workspaceId);
        String stateHash = state.calculateStateHash();

        List<MemberBalance> balances = settlementEngine.calculateBalances(state, workspace.getBaseCurrency());
        List<SettlementTransfer> transfers = settlementOptimizer.optimize(balances, workspace.getBaseCurrency());

        int version = sessionRepository.findByWorkspaceId(workspaceId).size() + 1;
        String sessionId = UUID.randomUUID().toString();

        SettlementSession session = new SettlementSession(sessionId, workspaceId, stateHash, version);
        sessionRepository.save(session);

        return new SettlementPlan(sessionId, workspaceId, transfers, stateHash, version);
    }

    public List<MemberBalance> getBalances(String workspaceId) {
        Workspace workspace = getWorkspace(workspaceId);
        WorkspaceFinancialState state = buildFinancialState(workspaceId);
        return settlementEngine.calculateBalances(state, workspace.getBaseCurrency());
    }

    public void confirmSettlement(String workspaceId, String transferId, String sessionId, String actorUserId) {
        Workspace workspace = getWorkspace(workspaceId);
        if (workspace.getStatus() == WorkspaceStatus.ARCHIVED) {
            throw new IllegalStateException("Cannot settle balances in an archived workspace");
        }

        SettlementSession session = sessionRepository.findById(sessionId)
                .orElseThrow(() -> new IllegalArgumentException("Settlement session not found"));

        WorkspaceFinancialState currentState = buildFinancialState(workspaceId);
        String currentHash = currentState.calculateStateHash();

        if (!session.getStateHash().equals(currentHash)) {
            throw new IllegalStateException("Settlement plan is outdated. Please regenerate.");
        }

        // Re-generate optimized transfers to find the matching transfer definition
        List<MemberBalance> balances = settlementEngine.calculateBalances(currentState, workspace.getBaseCurrency());
        List<SettlementTransfer> transfers = settlementOptimizer.optimize(balances, workspace.getBaseCurrency());

        SettlementTransfer target = transfers.stream()
                .filter(t -> t.id().equals(transferId))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Transfer recommendation not found in current plan"));

        // Create confirmed transaction
        SettlementTransaction transaction = new SettlementTransaction(
                UUID.randomUUID().toString(),
                workspaceId,
                sessionId,
                target.fromUserId(),
                target.toUserId(),
                target.amount()
        );
        transaction.setStatus(SettlementStatus.CONFIRMED);
        transaction.setConfirmedAt(Instant.now());

        transactionRepository.save(transaction);

        // Publish event
        eventPublisher.publishEvent(new SettlementConfirmedEvent(transaction));
    }

    public List<SettlementTransaction> getHistory(String workspaceId) {
        return transactionRepository.findByWorkspaceId(workspaceId).stream()
                .filter(st -> st.getStatus() == SettlementStatus.CONFIRMED)
                .collect(Collectors.toList());
    }
}
