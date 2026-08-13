package com.rkdevstudios.tripledger.settlement.domain;

import com.rkdevstudios.tripledger.contribution.domain.ContributionEntry;
import com.rkdevstudios.tripledger.contribution.domain.PlannedContribution;
import com.rkdevstudios.tripledger.expense.domain.Expense;
import com.rkdevstudios.tripledger.expense.domain.SplitAllocation;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Comparator;
import java.util.List;

public record WorkspaceFinancialState(
    String workspaceId,
    List<PlannedContribution> plannedContributions,
    List<ContributionEntry> contributionEntries,
    List<Expense> expenses,
    List<SplitAllocation> splitAllocations,
    List<SettlementTransaction> confirmedSettlements
) {
    public String calculateStateHash() {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            StringBuilder sb = new StringBuilder();
            sb.append(workspaceId);

            // Planned Contributions sorted by ID
            plannedContributions.stream()
                    .sorted(Comparator.comparing(PlannedContribution::getId))
                    .forEach(pc -> sb.append(pc.getId()).append(pc.getPlannedAmount()));

            // Contribution Entries sorted by ID
            contributionEntries.stream()
                    .sorted(Comparator.comparing(ContributionEntry::getId))
                    .forEach(ce -> sb.append(ce.getId()).append(ce.getAmount()));

            // Expenses sorted by ID
            expenses.stream()
                    .sorted(Comparator.comparing(Expense::getId))
                    .forEach(e -> sb.append(e.getId()).append(e.getMoney().getAmount()));

            // Split Allocations sorted by ID
            splitAllocations.stream()
                    .sorted(Comparator.comparing(SplitAllocation::getId))
                    .forEach(sa -> sb.append(sa.getId()).append(sa.getMoney().getAmount()));

            // Confirmed Settlements sorted by ID
            confirmedSettlements.stream()
                    .sorted(Comparator.comparing(SettlementTransaction::getId))
                    .forEach(st -> sb.append(st.getId()).append(st.getMoney().getAmount()).append(st.getStatus()));

            byte[] hash = digest.digest(sb.toString().getBytes(StandardCharsets.UTF_8));
            StringBuilder hexString = new StringBuilder();
            for (byte b : hash) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) {
                    hexString.append('0');
                }
                hexString.append(hex);
            }
            return hexString.toString();
        } catch (Exception e) {
            throw new RuntimeException("SHA-256 hashing failed", e);
        }
    }
}
