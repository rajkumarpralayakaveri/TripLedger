package com.rkdevstudios.tripledger.settlement.application;

import com.rkdevstudios.tripledger.contribution.application.ContributionService;
import com.rkdevstudios.tripledger.settlement.domain.SettlementConfirmedEvent;
import com.rkdevstudios.tripledger.settlement.domain.SettlementTransaction;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
public class SettlementContributionListener {

    private final ContributionService contributionService;

    public SettlementContributionListener(ContributionService contributionService) {
        this.contributionService = contributionService;
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void onSettlementConfirmed(SettlementConfirmedEvent event) {
        SettlementTransaction st = event.transaction();

        try {
            // 1. Debtor has paid their debt -> increases their contribution cash flow
            contributionService.recordCashContributionInternal(
                    st.getWorkspaceId(),
                    st.getFromUserId(),
                    st.getMoney().getAmount(),
                    "Repayment transfer to user: " + st.getToUserId()
            );

            // 2. Creditor received cash repayment -> offsets their net outstanding credits
            contributionService.recordAdjustmentInternal(
                    st.getWorkspaceId(),
                    st.getToUserId(),
                    st.getMoney().getAmount().negate(),
                    "Received repayment from user: " + st.getFromUserId(),
                    st.getId()
            );
        } catch (Exception e) {
            org.slf4j.LoggerFactory.getLogger(SettlementContributionListener.class)
                    .error("Failed to synchronize contribution entry for confirmed settlement {}", st.getId(), e);
            throw e;
        }
    }
}
