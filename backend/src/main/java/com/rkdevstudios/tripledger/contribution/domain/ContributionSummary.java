package com.rkdevstudios.tripledger.contribution.domain;

import java.math.BigDecimal;

/**
 * Computed value object representing a member's contribution state.
 * Never stored in the database — derived from ContributionEntry records.
 */
public record ContributionSummary(
    String workspaceId,
    String userId,
    BigDecimal plannedContribution,
    BigDecimal cashContributed,
    BigDecimal directExpenseContribution,
    BigDecimal adjustments,
    BigDecimal totalContribution,
    BigDecimal remainingContribution,
    ContributionStatus status
) {
    public static ContributionSummary compute(
            String workspaceId,
            String userId,
            BigDecimal plannedAmount,
            BigDecimal cash,
            BigDecimal directExpense,
            BigDecimal adjustments
    ) {
        BigDecimal total = cash.add(directExpense).add(adjustments);
        BigDecimal remaining = plannedAmount.subtract(total).max(BigDecimal.ZERO);
        ContributionStatus status = computeStatus(plannedAmount, total);

        return new ContributionSummary(
                workspaceId, userId, plannedAmount,
                cash, directExpense, adjustments,
                total, remaining, status
        );
    }

    private static ContributionStatus computeStatus(BigDecimal planned, BigDecimal total) {
        int cmp = total.compareTo(BigDecimal.ZERO);
        if (cmp == 0) return ContributionStatus.NOT_STARTED;
        if (total.compareTo(planned) > 0) return ContributionStatus.OVER_FUNDED;
        if (total.compareTo(planned) == 0) return ContributionStatus.FULLY_FUNDED;
        return ContributionStatus.PARTIALLY_FUNDED;
    }
}
