package com.rkdevstudios.tripledger.expense.application;

import com.rkdevstudios.tripledger.expense.domain.*;
import com.rkdevstudios.tripledger.identity.domain.User;
import com.rkdevstudios.tripledger.identity.domain.UserRepository;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Component
public class ActivityFeedListener {

    private final ActivityEntryRepository activityEntryRepository;
    private final UserRepository userRepository;

    public ActivityFeedListener(
            ActivityEntryRepository activityEntryRepository,
            UserRepository userRepository
    ) {
        this.activityEntryRepository = activityEntryRepository;
        this.userRepository = userRepository;
    }

    private String getUserName(String userId) {
        return userRepository.findById(userId)
                .map(User::getName)
                .orElse("Someone");
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void onExpenseCreated(ExpenseCreatedEvent event) {
        Expense expense = event.expense();
        String name = getUserName(expense.getPaidByUserId());
        String meta = "{\"description\":\"" + expense.getDescription() + "\"}";

        ActivityEntry entry = new ActivityEntry(
                UUID.randomUUID().toString(),
                expense.getWorkspaceId(),
                expense.getPaidByUserId(),
                ActivityType.EXPENSE_CREATED,
                meta
        );
        activityEntryRepository.save(entry);
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void onExpenseUpdated(ExpenseUpdatedEvent event) {
        Expense expense = event.newExpense();
        String meta = "{\"description\":\"" + expense.getDescription() + "\"}";

        ActivityEntry entry = new ActivityEntry(
                UUID.randomUUID().toString(),
                expense.getWorkspaceId(),
                expense.getPaidByUserId(),
                ActivityType.EXPENSE_UPDATED,
                meta
        );
        activityEntryRepository.save(entry);
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void onExpenseDeleted(ExpenseDeletedEvent event) {
        Expense expense = event.expense();
        String meta = "{\"description\":\"" + expense.getDescription() + "\",\"reason\":\"" + event.reason() + "\"}";

        ActivityEntry entry = new ActivityEntry(
                UUID.randomUUID().toString(),
                expense.getWorkspaceId(),
                event.actorUserId(),
                ActivityType.EXPENSE_DELETED,
                meta
        );
        activityEntryRepository.save(entry);
    }
}
