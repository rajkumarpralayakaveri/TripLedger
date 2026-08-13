package com.rkdevstudios.tripledger.settlement.domain;

import com.rkdevstudios.tripledger.contribution.domain.ContributionEntry;
import com.rkdevstudios.tripledger.contribution.domain.PlannedContribution;
import com.rkdevstudios.tripledger.expense.domain.Money;
import com.rkdevstudios.tripledger.expense.domain.SplitAllocation;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;

@Component
public class SettlementEngine {

    public List<MemberBalance> calculateBalances(WorkspaceFinancialState state, String currency) {
        Map<String, BigDecimal> paidMap = new HashMap<>();
        Map<String, BigDecimal> owedMap = new HashMap<>();

        // Initialize maps with zero for all planned members
        Set<String> allUsers = new HashSet<>();
        for (PlannedContribution pc : state.plannedContributions()) {
            allUsers.add(pc.getUserId());
            paidMap.put(pc.getUserId(), BigDecimal.ZERO);
            owedMap.put(pc.getUserId(), BigDecimal.ZERO);
        }

        // Sum up actual contributions
        for (ContributionEntry ce : state.contributionEntries()) {
            allUsers.add(ce.getUserId());
            BigDecimal current = paidMap.getOrDefault(ce.getUserId(), BigDecimal.ZERO);
            paidMap.put(ce.getUserId(), current.add(ce.getAmount()));
        }

        // Sum up split allocations owed
        for (SplitAllocation sa : state.splitAllocations()) {
            allUsers.add(sa.getUserId());
            BigDecimal current = owedMap.getOrDefault(sa.getUserId(), BigDecimal.ZERO);
            owedMap.put(sa.getUserId(), current.add(sa.getMoney().getAmount()));
        }

        // Build list of MemberBalance
        List<MemberBalance> balances = new ArrayList<>();
        BigDecimal sumBalances = BigDecimal.ZERO;

        for (String userId : allUsers) {
            BigDecimal paid = paidMap.getOrDefault(userId, BigDecimal.ZERO).setScale(4, RoundingMode.HALF_UP);
            BigDecimal owed = owedMap.getOrDefault(userId, BigDecimal.ZERO).setScale(4, RoundingMode.HALF_UP);
            BigDecimal balance = paid.subtract(owed).setScale(4, RoundingMode.HALF_UP);

            balances.add(new MemberBalance(
                    userId,
                    new Money(paid, currency),
                    new Money(owed, currency),
                    new Money(balance, currency)
            ));

            sumBalances = sumBalances.add(balance);
        }

        // Validate Invariant 1: Sum(Member Balances) == 0 (with minor tolerance for division remainders, e.g. < 0.01)
        if (sumBalances.abs().compareTo(BigDecimal.valueOf(0.01)) > 0) {
            throw new IllegalStateException("System Invariant Violated: Sum of member balances is not zero (Sum: " + sumBalances + ")");
        }

        return balances;
    }
}
