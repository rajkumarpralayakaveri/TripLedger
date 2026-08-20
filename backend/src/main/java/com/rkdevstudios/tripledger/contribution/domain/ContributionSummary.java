package com.rkdevstudios.tripledger.contribution.domain;

import com.rkdevstudios.tripledger.workspace.domain.MemberRole;
import java.math.BigDecimal;

/**
 * Computed value object representing a member's contribution state.
 * Never stored in the database — derived from ContributionEntry records.
 */
public record ContributionSummary(
    String workspaceId,
    String userId,
    String name,
    MemberRole role,
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
            String name,
            MemberRole role,
            BigDecimal plannedAmount,
            BigDecimal cash,
            BigDecimal directExpense,
            BigDecimal adjustments
    ) {
        return compute(
                workspaceId, userId, name, role, plannedAmount,
                cash, directExpense, adjustments,
                com.rkdevstudios.tripledger.workspace.domain.ContributionMode.COMBINED,
                BigDecimal.ZERO
        );
    }

    public static ContributionSummary compute(
            String workspaceId,
            String userId,
            String name,
            MemberRole role,
            BigDecimal plannedAmount,
            BigDecimal cash,
            BigDecimal directExpense,
            BigDecimal adjustments,
            com.rkdevstudios.tripledger.workspace.domain.ContributionMode mode,
            BigDecimal consumedSplit
    ) {
        BigDecimal total;
        BigDecimal remaining;

        if (mode == com.rkdevstudios.tripledger.workspace.domain.ContributionMode.INDIVIDUAL) {
            // For INDIVIDUAL mode: total contribution = consumed share of shared expenses.
            // Remaining personal budget = planned amount - consumed share.
            total = consumedSplit != null ? consumedSplit : BigDecimal.ZERO;
            remaining = plannedAmount.subtract(total).max(BigDecimal.ZERO);
        } else {
            // For COMBINED mode: total contribution = cash + direct expense + adjustments.
            total = cash.add(directExpense).add(adjustments);
            remaining = plannedAmount.subtract(total).max(BigDecimal.ZERO);
        }

        ContributionStatus status = computeStatus(plannedAmount, total);

        return new ContributionSummary(
                workspaceId, userId, name, role, plannedAmount,
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
