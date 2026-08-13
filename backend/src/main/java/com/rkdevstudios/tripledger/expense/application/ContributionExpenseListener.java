package com.rkdevstudios.tripledger.expense.application;

import com.rkdevstudios.tripledger.contribution.application.ContributionService;
import com.rkdevstudios.tripledger.expense.domain.Expense;
import com.rkdevstudios.tripledger.expense.domain.ExpenseCreatedEvent;
import com.rkdevstudios.tripledger.expense.domain.ExpenseDeletedEvent;
import com.rkdevstudios.tripledger.expense.domain.ExpenseUpdatedEvent;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

@Component
public class ContributionExpenseListener {

    private final ContributionService contributionService;

    public ContributionExpenseListener(ContributionService contributionService) {
        this.contributionService = contributionService;
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void onExpenseCreated(ExpenseCreatedEvent event) {
        Expense expense = event.expense();
        contributionService.recordDirectExpense(
                expense.getWorkspaceId(),
                expense.getPaidByUserId(),
                expense.getMoney().getAmount(),
                "Direct Expense: " + expense.getDescription(),
                expense.getId()
        );
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void onExpenseUpdated(ExpenseUpdatedEvent event) {
        Expense old = event.oldExpense();
        Expense updated = event.newExpense();

        // 1. Negate the old expense amount via ADJUSTMENT
        contributionService.recordAdjustment(
                old.getWorkspaceId(),
                old.getPaidByUserId(),
                old.getMoney().getAmount().negate(),
                "Expense updated correction for description: " + old.getDescription(),
                old.getPaidByUserId()
        );

        // 2. Record the new updated expense amount as a DIRECT_EXPENSE
        contributionService.recordDirectExpense(
                updated.getWorkspaceId(),
                updated.getPaidByUserId(),
                updated.getMoney().getAmount(),
                "Direct Expense (Updated): " + updated.getDescription(),
                updated.getId()
        );
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void onExpenseDeleted(ExpenseDeletedEvent event) {
        Expense expense = event.expense();
        // Negate the deleted expense amount via ADJUSTMENT
        contributionService.recordAdjustment(
                expense.getWorkspaceId(),
                expense.getPaidByUserId(),
                expense.getMoney().getAmount().negate(),
                "Expense deleted correction: " + event.reason(),
                event.actorUserId()
        );
    }
}
