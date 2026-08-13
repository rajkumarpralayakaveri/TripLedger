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

        // 1. Debtor has paid their debt -> increases their contribution cash flow
        contributionService.recordCashContribution(
                st.getWorkspaceId(),
                st.getFromUserId(),
                st.getMoney().getAmount(),
                "Repayment transfer to user: " + st.getToUserId(),
                st.getFromUserId()
        );

        // 2. Creditor received cash repayment -> offsets their net outstanding credits
        contributionService.recordAdjustment(
                st.getWorkspaceId(),
                st.getToUserId(),
                st.getMoney().getAmount().negate(),
                "Received repayment from user: " + st.getFromUserId(),
                st.getToUserId()
        );
    }
}
