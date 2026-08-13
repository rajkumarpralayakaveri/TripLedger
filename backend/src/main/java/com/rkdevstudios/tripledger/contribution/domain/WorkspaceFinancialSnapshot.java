package com.rkdevstudios.tripledger.contribution.domain;

import java.math.BigDecimal;
import java.util.List;

/**
 * Full dashboard aggregate — one value object, one API call.
 * Eliminates the need for Android to call Budget + Fund + Contributions separately.
 */
public record WorkspaceFinancialSnapshot(
    String workspaceId,

    // Budget Ledger
    BigDecimal totalBudget,
    BigDecimal totalSpent,
    BigDecimal remainingBudget,

    // Fund Ledger
    BigDecimal currentFund,
    BigDecimal fundingGap,

    // Member Summary
    int memberCount,
    int fundedMembers,
    int pendingMembers,
    int overFundedMembers,

    // Per-member detail
    List<ContributionSummary> memberContributions
) {
    public static WorkspaceFinancialSnapshot of(
            String workspaceId,
            BigDecimal totalBudget,
            BigDecimal totalSpent,
            BigDecimal currentFund,
            List<ContributionSummary> memberContributions
    ) {
        BigDecimal remainingBudget = totalBudget.subtract(totalSpent).max(BigDecimal.ZERO);
        BigDecimal fundingGap = totalBudget.subtract(currentFund).max(BigDecimal.ZERO);

        int funded = 0;
        int pending = 0;
        int overFunded = 0;

        for (ContributionSummary s : memberContributions) {
            switch (s.status()) {
                case FULLY_FUNDED -> funded++;
                case OVER_FUNDED -> overFunded++;
                default -> pending++;
            }
        }

        return new WorkspaceFinancialSnapshot(
                workspaceId,
                totalBudget, totalSpent, remainingBudget,
                currentFund, fundingGap,
                memberContributions.size(), funded, pending, overFunded,
                memberContributions
        );
    }
}
