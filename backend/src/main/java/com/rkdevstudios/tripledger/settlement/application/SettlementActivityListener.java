package com.rkdevstudios.tripledger.settlement.application;

import com.rkdevstudios.tripledger.expense.domain.ActivityEntry;
import com.rkdevstudios.tripledger.expense.domain.ActivityEntryRepository;
import com.rkdevstudios.tripledger.expense.domain.ActivityType;
import com.rkdevstudios.tripledger.identity.domain.User;
import com.rkdevstudios.tripledger.identity.domain.UserRepository;
import com.rkdevstudios.tripledger.settlement.domain.SettlementConfirmedEvent;
import com.rkdevstudios.tripledger.settlement.domain.SettlementTransaction;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import java.util.UUID;

@Component
public class SettlementActivityListener {

    private final ActivityEntryRepository activityEntryRepository;
    private final UserRepository userRepository;

    public SettlementActivityListener(
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
    public void onSettlementConfirmed(SettlementConfirmedEvent event) {
        SettlementTransaction st = event.transaction();
        String recipientName = getUserName(st.getToUserId());

        String meta = "{\"description\":\"repayment of " + st.getMoney().getCurrency() + " " + st.getMoney().getAmount() + " to " + recipientName + "\"}";

        ActivityEntry entry = new ActivityEntry(
                UUID.randomUUID().toString(),
                st.getWorkspaceId(),
                st.getFromUserId(),
                ActivityType.SETTLEMENT_CONFIRMED,
                meta
        );
        activityEntryRepository.save(entry);
    }
}
