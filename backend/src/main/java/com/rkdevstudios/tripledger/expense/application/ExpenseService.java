package com.rkdevstudios.tripledger.expense.application;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.rkdevstudios.tripledger.expense.domain.*;
import com.rkdevstudios.tripledger.workspace.domain.Workspace;
import com.rkdevstudios.tripledger.workspace.domain.WorkspaceMember;
import com.rkdevstudios.tripledger.workspace.domain.WorkspaceMemberRepository;
import com.rkdevstudios.tripledger.workspace.domain.WorkspaceRepository;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.*;

@Service
@Transactional(readOnly = true)
public class ExpenseService {

    private final ExpenseRepository expenseRepository;
    private final SplitAllocationRepository splitAllocationRepository;
    private final ExpenseHistoryRepository expenseHistoryRepository;
    private final WorkspaceRepository workspaceRepository;
    private final WorkspaceMemberRepository workspaceMemberRepository;
    private final SplitCalculationService splitCalculationService;
    private final ApplicationEventPublisher eventPublisher;
    private final ObjectMapper objectMapper = new ObjectMapper()
            .registerModule(new com.fasterxml.jackson.datatype.jsr310.JavaTimeModule());

    public ExpenseService(
            ExpenseRepository expenseRepository,
            SplitAllocationRepository splitAllocationRepository,
            ExpenseHistoryRepository expenseHistoryRepository,
            WorkspaceRepository workspaceRepository,
            WorkspaceMemberRepository workspaceMemberRepository,
            SplitCalculationService splitCalculationService,
            ApplicationEventPublisher eventPublisher
    ) {
        this.expenseRepository = expenseRepository;
        this.splitAllocationRepository = splitAllocationRepository;
        this.expenseHistoryRepository = expenseHistoryRepository;
        this.workspaceRepository = workspaceRepository;
        this.workspaceMemberRepository = workspaceMemberRepository;
        this.splitCalculationService = splitCalculationService;
        this.eventPublisher = eventPublisher;
    }

    private void verifyMember(String workspaceId, String userId) {
        workspaceMemberRepository.findByWorkspaceIdAndUserId(workspaceId, userId)
                .orElseThrow(() -> new SecurityException("User " + userId + " is not a member of workspace " + workspaceId));
    }

    private Workspace getWorkspaceAndVerifyCurrency(String workspaceId, String currency) {
        Workspace ws = workspaceRepository.findById(workspaceId)
                .orElseThrow(() -> new IllegalArgumentException("Workspace not found"));
        if (ws.getStatus() == com.rkdevstudios.tripledger.workspace.domain.WorkspaceStatus.ARCHIVED) {
            throw new IllegalStateException("Cannot add or modify expenses in an archived workspace");
        }
        if (!ws.getBaseCurrency().equalsIgnoreCase(currency)) {
            throw new IllegalArgumentException("Expense currency (" + currency + ") must match workspace base currency (" + ws.getBaseCurrency() + ")");
        }
        return ws;
    }

    private void verifyCreatorOrAdmin(Expense expense, String callerUserId) {
        WorkspaceMember member = workspaceMemberRepository.findByWorkspaceIdAndUserId(expense.getWorkspaceId(), callerUserId)
                .orElseThrow(() -> new SecurityException("User " + callerUserId + " is not a member of workspace " + expense.getWorkspaceId()));

        if (member.getRole() == com.rkdevstudios.tripledger.workspace.domain.MemberRole.ADMIN) {
            return; // Admin can modify any expense
        }

        String creatorId = expense.getCreatedByUserId();
        if (creatorId == null) {
            // Backfill creatorId from database audit history
            creatorId = expenseHistoryRepository.findByExpenseId(expense.getId()).stream()
                    .filter(h -> "CREATE".equalsIgnoreCase(h.getAction()))
                    .map(ExpenseHistory::getActorUserId)
                    .findFirst()
                    .orElse(expense.getPaidByUserId()); // Fallback to paidByUserId if no CREATE log exists
            expense.setCreatedByUserId(creatorId);
            expenseRepository.save(expense);
        }

        if (!creatorId.equals(callerUserId)) {
            throw new SecurityException("User " + callerUserId + " is not authorized to modify this expense");
        }
    }

    private String serializeExpenseState(Expense expense, List<SplitAllocation> allocations) {
        try {
            Map<String, Object> state = new HashMap<>();
            state.put("expense", expense);
            state.put("allocations", allocations);
            return objectMapper.writeValueAsString(state);
        } catch (Exception e) {
            return "{}";
        }
    }

    @Transactional
    public Expense createExpense(
            String workspaceId,
            String paidByUserId,
            BigDecimal amount,
            String currency,
            String description,
            String categoryId,
            LocalDate expenseDate,
            SplitType splitType,
            List<String> participantIds,
            Map<String, BigDecimal> splitValues,
            String callerUserId
    ) {
        return createExpense(
                workspaceId, paidByUserId, amount, currency, description, categoryId,
                expenseDate, null, null, null, splitType, participantIds, splitValues, callerUserId
        );
    }

    @Transactional
    public Expense createExpense(
            String workspaceId,
            String paidByUserId,
            BigDecimal amount,
            String currency,
            String description,
            String categoryId,
            LocalDate expenseDate,
            java.time.Instant expenseAt,
            String receiptUrl,
            String note,
            SplitType splitType,
            List<String> participantIds,
            Map<String, BigDecimal> splitValues,
            String callerUserId
    ) {
        // Enforce membership & currency validations
        verifyMember(workspaceId, callerUserId);
        verifyMember(workspaceId, paidByUserId);
        getWorkspaceAndVerifyCurrency(workspaceId, currency);

        if (expenseDate.isAfter(LocalDate.now())) {
            throw new IllegalArgumentException("Expense date cannot be in the future");
        }
        if (participantIds == null || participantIds.isEmpty()) {
            throw new IllegalArgumentException("Expense must have at least one participant");
        }

        // Verify split total matches amount exactly using Split Calculation Engine
        Map<String, BigDecimal> calculatedShares = splitCalculationService.calculateSplits(
                amount,
                splitType,
                participantIds,
                splitValues
        );

        String expenseId = UUID.randomUUID().toString();
        Money expenseMoney = new Money(amount, currency);

        Expense expense = new Expense(
                expenseId,
                workspaceId,
                paidByUserId,
                expenseMoney,
                description,
                categoryId,
                expenseDate,
                expenseAt,
                receiptUrl,
                note,
                splitType,
                callerUserId
        );
        expense.setStatus(ExpenseStatus.UNSETTLED);
        expense.setExpenseType(ExpenseType.NORMAL);

        Expense savedExpense = expenseRepository.save(expense);

        // Save allocations
        List<SplitAllocation> savedAllocations = new ArrayList<>();
        for (String participantId : participantIds) {
            verifyMember(workspaceId, participantId);
            BigDecimal shareAmount = calculatedShares.get(participantId);
            Money shareMoney = new Money(shareAmount, currency);

            SplitAllocation allocation = new SplitAllocation(
                    UUID.randomUUID().toString(),
                    expenseId,
                    participantId,
                    shareMoney,
                    splitValues != null ? splitValues.getOrDefault(participantId, BigDecimal.ZERO) : BigDecimal.ZERO
            );
            savedAllocations.add(splitAllocationRepository.save(allocation));
        }

        // Log history audit trail
        String afterJson = serializeExpenseState(savedExpense, savedAllocations);
        ExpenseHistory history = new ExpenseHistory(
                UUID.randomUUID().toString(),
                expenseId,
                "CREATE",
                null,
                afterJson,
                callerUserId,
                "Initial Creation"
        );
        expenseHistoryRepository.save(history);

        // Publish event to other ledger modules (Contribution, Activity Feed, etc.)
        eventPublisher.publishEvent(new ExpenseCreatedEvent(savedExpense));

        return savedExpense;
    }

    @Transactional
    public Expense updateExpense(
            String expenseId,
            BigDecimal amount,
            String currency,
            String description,
            String categoryId,
            LocalDate expenseDate,
            SplitType splitType,
            List<String> participantIds,
            Map<String, BigDecimal> splitValues,
            String reason,
            String callerUserId
    ) {
        Expense expense = expenseRepository.findById(expenseId)
                .orElseThrow(() -> new IllegalArgumentException("Expense not found"));

        if (expense.getStatus() == ExpenseStatus.DELETED) {
            throw new IllegalStateException("Cannot update a deleted expense");
        }
        if (expense.getStatus() == ExpenseStatus.SETTLED) {
            throw new IllegalStateException("Cannot update a settled expense");
        }

        String workspaceId = expense.getWorkspaceId();
        verifyMember(workspaceId, callerUserId);
        verifyCreatorOrAdmin(expense, callerUserId);
        getWorkspaceAndVerifyCurrency(workspaceId, currency);

        if (expenseDate.isAfter(LocalDate.now())) {
            throw new IllegalArgumentException("Expense date cannot be in the future");
        }
        if (participantIds == null || participantIds.isEmpty()) {
            throw new IllegalArgumentException("Expense must have at least one participant");
        }
        if (reason == null || reason.isBlank()) {
            throw new IllegalArgumentException("Update action requires a reason");
        }

        // Deep copy of old state for audit logging
        List<SplitAllocation> oldAllocations = splitAllocationRepository.findByExpenseId(expenseId);
        String beforeJson = serializeExpenseState(expense, oldAllocations);

        // Perform splits recalculations
        Map<String, BigDecimal> calculatedShares = splitCalculationService.calculateSplits(
                amount,
                splitType,
                participantIds,
                splitValues
        );

        expense.setMoney(new Money(amount, currency));
        expense.setDescription(description);
        expense.setCategoryId(categoryId);
        expense.setExpenseDate(expenseDate);
        expense.setSplitType(splitType);
        Expense updatedExpense = expenseRepository.save(expense);

        // Reset and save new allocations
        splitAllocationRepository.deleteByExpenseId(expenseId);
        List<SplitAllocation> newAllocations = new ArrayList<>();
        for (String participantId : participantIds) {
            verifyMember(workspaceId, participantId);
            BigDecimal shareAmount = calculatedShares.get(participantId);
            Money shareMoney = new Money(shareAmount, currency);

            SplitAllocation allocation = new SplitAllocation(
                    UUID.randomUUID().toString(),
                    expenseId,
                    participantId,
                    shareMoney,
                    splitValues != null ? splitValues.getOrDefault(participantId, BigDecimal.ZERO) : BigDecimal.ZERO
            );
            newAllocations.add(splitAllocationRepository.save(allocation));
        }

        // Save history audit trail
        String afterJson = serializeExpenseState(updatedExpense, newAllocations);
        ExpenseHistory history = new ExpenseHistory(
                UUID.randomUUID().toString(),
                expenseId,
                "UPDATE",
                beforeJson,
                afterJson,
                callerUserId,
                reason
        );
        expenseHistoryRepository.save(history);

        // Publish event to recalculate contribution ledgers & logs
        eventPublisher.publishEvent(new ExpenseUpdatedEvent(expense, updatedExpense));

        return updatedExpense;
    }

    @Transactional
    public void deleteExpense(String expenseId, String reason, String callerUserId) {
        Expense expense = expenseRepository.findById(expenseId)
                .orElseThrow(() -> new IllegalArgumentException("Expense not found"));

        if (expense.getStatus() == ExpenseStatus.DELETED) {
            throw new IllegalStateException("Expense is already deleted");
        }
        if (expense.getStatus() == ExpenseStatus.SETTLED) {
            throw new IllegalStateException("Cannot delete a settled expense");
        }
        if (reason == null || reason.isBlank()) {
            throw new IllegalArgumentException("Delete action requires a reason");
        }

        verifyMember(expense.getWorkspaceId(), callerUserId);
        verifyCreatorOrAdmin(expense, callerUserId);

        // Capture state before soft-deleting
        List<SplitAllocation> oldAllocations = splitAllocationRepository.findByExpenseId(expenseId);
        String beforeJson = serializeExpenseState(expense, oldAllocations);

        expense.setStatus(ExpenseStatus.DELETED);
        Expense deletedExpense = expenseRepository.save(expense);

        // Delete split allocations cleanly to prevent balance/settlement invariant failures
        splitAllocationRepository.deleteByExpenseId(expenseId);

        String afterJson = serializeExpenseState(deletedExpense, oldAllocations);

        ExpenseHistory history = new ExpenseHistory(
                UUID.randomUUID().toString(),
                expenseId,
                "DELETE",
                beforeJson,
                afterJson,
                callerUserId,
                reason
        );
        expenseHistoryRepository.save(history);

        // Publish deleted event
        eventPublisher.publishEvent(new ExpenseDeletedEvent(expense, reason, callerUserId));
    }

    public List<Expense> getWorkspaceExpenses(String workspaceId, String callerUserId) {
        verifyMember(workspaceId, callerUserId);
        return expenseRepository.findByWorkspaceIdAndStatusNot(workspaceId, ExpenseStatus.DELETED);
    }
}
