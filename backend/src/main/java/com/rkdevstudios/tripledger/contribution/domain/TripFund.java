package com.rkdevstudios.tripledger.contribution.domain;

import java.math.BigDecimal;

/**
 * Workspace-level fund view. Computed from all ContributionEntry records.
 * Never stored — derived on demand.
 */
public record TripFund(
    String workspaceId,
    BigDecimal totalBudget,
    BigDecimal totalCashContributions,
    BigDecimal totalDirectExpenses,
    BigDecimal totalAdjustments,
    BigDecimal currentFund,
    BigDecimal fundingGap
) {
    public static TripFund compute(
            String workspaceId,
            BigDecimal totalBudget,
            BigDecimal cash,
            BigDecimal directExpenses,
            BigDecimal adjustments
    ) {
        BigDecimal currentFund = cash.add(directExpenses).add(adjustments);
        BigDecimal fundingGap = totalBudget.subtract(currentFund).max(BigDecimal.ZERO);
        return new TripFund(workspaceId, totalBudget, cash, directExpenses, adjustments, currentFund, fundingGap);
    }
}
