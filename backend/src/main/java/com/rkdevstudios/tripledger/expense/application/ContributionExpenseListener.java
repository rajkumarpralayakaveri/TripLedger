package com.rkdevstudios.tripledger.expense.application;

import com.rkdevstudios.tripledger.contribution.application.ContributionService;
import com.rkdevstudios.tripledger.expense.domain.Expense;
import com.rkdevstudios.tripledger.expense.domain.ExpenseCreatedEvent;
import com.rkdevstudios.tripledger.expense.domain.ExpenseDeletedEvent;
import com.rkdevstudios.tripledger.expense.domain.ExpenseUpdatedEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

@Component
public class ContributionExpenseListener {

    private static final Logger log = LoggerFactory.getLogger(ContributionExpenseListener.class);
    private final ContributionService contributionService;

    public ContributionExpenseListener(ContributionService contributionService) {
        this.contributionService = contributionService;
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void onExpenseCreated(ExpenseCreatedEvent event) {
        Expense expense = event.expense();
        try {
            contributionService.recordDirectExpense(
                    expense.getWorkspaceId(),
                    expense.getPaidByUserId(),
                    expense.getMoney().getAmount(),
                    "Direct Expense: " + expense.getDescription(),
                    expense.getId()
            );
        } catch (Exception e) {
            log.error("Failed to synchronize contribution ledger for expense {}", expense.getId(), e);
            throw e;
        }
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void onExpenseUpdated(ExpenseUpdatedEvent event) {
        Expense old = event.oldExpense();
        Expense updated = event.newExpense();

        try {
            // 1. Negate the old expense amount via ADJUSTMENT internally without admin checks
            contributionService.recordAdjustmentInternal(
                    old.getWorkspaceId(),
                    old.getPaidByUserId(),
                    old.getMoney().getAmount().negate(),
                    "Expense updated correction: " + old.getDescription(),
                    old.getId()
            );

            // 2. Record the new updated expense amount as a DIRECT_EXPENSE
            contributionService.recordDirectExpense(
                    updated.getWorkspaceId(),
                    updated.getPaidByUserId(),
                    updated.getMoney().getAmount(),
                    "Direct Expense (Updated): " + updated.getDescription(),
                    updated.getId()
            );
        } catch (Exception e) {
            log.error("Failed to synchronize contribution ledger update for expense {}", updated.getId(), e);
            throw e;
        }
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void onExpenseDeleted(ExpenseDeletedEvent event) {
        Expense expense = event.expense();
        try {
            // Negate the deleted expense amount via ADJUSTMENT internally without admin checks
            contributionService.recordAdjustmentInternal(
                    expense.getWorkspaceId(),
                    expense.getPaidByUserId(),
                    expense.getMoney().getAmount().negate(),
                    "Expense deleted correction: " + event.reason(),
                    expense.getId()
            );
        } catch (Exception e) {
            log.error("Failed to synchronize contribution ledger deletion for expense {}", expense.getId(), e);
            throw e;
        }
    }
}
