package com.rkdevstudios.tripledger.contribution.application;

import com.rkdevstudios.tripledger.contribution.domain.ExpensePaidEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

@Component
public class ExpensePaidEventListener {

    private final ContributionService contributionService;

    public ExpensePaidEventListener(ContributionService contributionService) {
        this.contributionService = contributionService;
    }

    @EventListener
    public void onExpensePaid(ExpensePaidEvent event) {
        contributionService.recordDirectExpense(
                event.workspaceId(),
                event.paidByUserId(),
                event.amount(),
                event.description(),
                event.expenseId()
        );
    }
}
