package com.rkdevstudios.tripledger.settlement;

import com.rkdevstudios.tripledger.contribution.domain.ContributionEntry;
import com.rkdevstudios.tripledger.contribution.domain.ContributionEntryType;
import com.rkdevstudios.tripledger.contribution.domain.PlannedContribution;
import com.rkdevstudios.tripledger.expense.domain.Expense;
import com.rkdevstudios.tripledger.expense.domain.Money;
import com.rkdevstudios.tripledger.expense.domain.SplitAllocation;
import com.rkdevstudios.tripledger.settlement.domain.MemberBalance;
import com.rkdevstudios.tripledger.settlement.domain.SettlementEngine;
import com.rkdevstudios.tripledger.settlement.domain.WorkspaceFinancialState;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class SettlementEngineTest {

    private final SettlementEngine settlementEngine = new SettlementEngine();

    @Test
    public void testCalculateBalancesInvariantSuccess() {
        // Setup state
        PlannedContribution pc1 = new PlannedContribution("pc1", "ws1", "Raj", BigDecimal.valueOf(10000));
        PlannedContribution pc2 = new PlannedContribution("pc2", "ws1", "Amit", BigDecimal.valueOf(10000));

        ContributionEntry ce1 = new ContributionEntry("ce1", "ws1", "Raj", ContributionEntryType.CASH, BigDecimal.valueOf(12000), "Desc", "actor");
        ContributionEntry ce2 = new ContributionEntry("ce2", "ws1", "Amit", ContributionEntryType.CASH, BigDecimal.valueOf(8000), "Desc", "actor");

        SplitAllocation sa1 = new SplitAllocation("sa1", "e1", "Raj", new Money(BigDecimal.valueOf(10000), "INR"), BigDecimal.valueOf(10000));
        SplitAllocation sa2 = new SplitAllocation("sa2", "e1", "Amit", new Money(BigDecimal.valueOf(10000), "INR"), BigDecimal.valueOf(10000));

        WorkspaceFinancialState state = new WorkspaceFinancialState(
                "ws1",
                List.of(pc1, pc2),
                List.of(ce1, ce2),
                Collections.emptyList(),
                List.of(sa1, sa2),
                Collections.emptyList()
        );

        List<MemberBalance> balances = settlementEngine.calculateBalances(state, "INR");

        assertEquals(2, balances.size());

        MemberBalance rajBal = balances.stream().filter(b -> b.userId().equals("Raj")).findFirst().orElseThrow();
        assertEquals(BigDecimal.valueOf(12000).setScale(4), rajBal.paid().getAmount());
        assertEquals(BigDecimal.valueOf(10000).setScale(4), rajBal.owed().getAmount());
        assertEquals(BigDecimal.valueOf(2000).setScale(4), rajBal.balance().getAmount());

        MemberBalance amitBal = balances.stream().filter(b -> b.userId().equals("Amit")).findFirst().orElseThrow();
        assertEquals(BigDecimal.valueOf(8000).setScale(4), amitBal.paid().getAmount());
        assertEquals(BigDecimal.valueOf(10000).setScale(4), amitBal.owed().getAmount());
        assertEquals(BigDecimal.valueOf(-2000).setScale(4), amitBal.balance().getAmount());
    }

    @Test
    public void testInvariantsViolationThrowsException() {
        // Setup state where balances don't sum to zero (e.g. invalid split totals)
        PlannedContribution pc1 = new PlannedContribution("pc1", "ws1", "Raj", BigDecimal.valueOf(10000));
        ContributionEntry ce1 = new ContributionEntry("ce1", "ws1", "Raj", ContributionEntryType.CASH, BigDecimal.valueOf(12000), "Desc", "actor");
        SplitAllocation sa1 = new SplitAllocation("sa1", "e1", "Raj", new Money(BigDecimal.valueOf(5000), "INR"), BigDecimal.valueOf(5000)); // Not matching contribution flow sum of zero

        WorkspaceFinancialState state = new WorkspaceFinancialState(
                "ws1",
                List.of(pc1),
                List.of(ce1),
                Collections.emptyList(),
                List.of(sa1),
                Collections.emptyList()
        );

        assertThrows(IllegalStateException.class, () -> {
            settlementEngine.calculateBalances(state, "INR");
        });
    }

    @Test
    public void testCalculateBalances_AccountingInvariantsAndFiltering() {
        // Scenario 1: Active Expense (DIRECT_EXPENSE + split allocations included)
        // Scenario 2: Deleted expense with orphan DIRECT_EXPENSE (direct expense is present but allocations are missing, filtered out during buildFinancialState but simulated here)
        // Scenario 3: Deleted expense with matching deletion adjustment (both are filtered out or balanced)
        // Scenario 4: Unrelated CASH contribution (included)
        // Scenario 5: Unrelated/non-expense ADJUSTMENT (included)
        // Let's set up the test entries to balance exactly:
        PlannedContribution pcA = new PlannedContribution("pc1", "ws1", "Raj", BigDecimal.valueOf(1000));
        PlannedContribution pcB = new PlannedContribution("pc2", "ws1", "Amit", BigDecimal.valueOf(1000));

        ContributionEntry e1Paid = new ContributionEntry("ce1", "ws1", "Raj", ContributionEntryType.DIRECT_EXPENSE, BigDecimal.valueOf(1000), "Lunch", "e1");
        SplitAllocation e1OwedRaj = new SplitAllocation("sa1", "e1", "Raj", new Money(BigDecimal.valueOf(500), "INR"), BigDecimal.valueOf(1));
        SplitAllocation e1OwedAmit = new SplitAllocation("sa2", "e1", "Amit", new Money(BigDecimal.valueOf(500), "INR"), BigDecimal.valueOf(1));

        // CASH repayment: Amit pays Raj 500.
        ContributionEntry repaymentPaid = new ContributionEntry("ce2", "ws1", "Amit", ContributionEntryType.CASH, BigDecimal.valueOf(500), "Settle", null);
        ContributionEntry repaymentReceived = new ContributionEntry("ce3", "ws1", "Raj", ContributionEntryType.ADJUSTMENT, BigDecimal.valueOf(-500), "Settle adjustment", null);

        // Scenario 5: Unrelated/non-expense ADJUSTMENT (must balance: let's say a manual adjustment of +100 to Raj's contribution offset by -100 to Amit's contribution)
        ContributionEntry unrelatedAdjRaj = new ContributionEntry("ce4", "ws1", "Raj", ContributionEntryType.ADJUSTMENT, BigDecimal.valueOf(100), "Manual adjustment Raj", null);
        ContributionEntry unrelatedAdjAmit = new ContributionEntry("ce5", "ws1", "Amit", ContributionEntryType.ADJUSTMENT, BigDecimal.valueOf(-100), "Manual adjustment Amit", null);

        WorkspaceFinancialState balancedState = new WorkspaceFinancialState(
                "ws1",
                List.of(pcA, pcB),
                List.of(e1Paid, repaymentPaid, repaymentReceived, unrelatedAdjRaj, unrelatedAdjAmit),
                Collections.emptyList(),
                List.of(e1OwedRaj, e1OwedAmit),
                Collections.emptyList()
        );

        List<MemberBalance> balancedBalances = settlementEngine.calculateBalances(balancedState, "INR");
        BigDecimal balancedSum = balancedBalances.stream()
                .map(mb -> mb.balance().getAmount())
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        assertEquals(0, balancedSum.setScale(4).compareTo(BigDecimal.ZERO.setScale(4)));
    }
}
