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

    @Test
    public void testOptimizeCoffeeExpenseWithUpdateScenario() {
        // Tracing:
        // Payer: b9c08de4-a885-4c63-aa5b-d4be916e4ade (Paid net contribution = +100, Owed split allocation = 33.3333, Balance = +66.6667)
        // Participant 1: 79f089e9-9bd3-4dd0-ac61-fa40435ba399 (Paid = 0, Owed = 33.3333, Balance = -33.3333)
        // Participant 2: e732ab7b-9d12-40ce-b19b-330c36871f1f (Paid = 0, Owed = 33.3334, Balance = -33.3334)
        
        PlannedContribution pc1 = new PlannedContribution("pc1", "ws1", "b9c08de4-a885-4c63-aa5b-d4be916e4ade", BigDecimal.valueOf(100));
        PlannedContribution pc2 = new PlannedContribution("pc2", "ws1", "79f089e9-9bd3-4dd0-ac61-fa40435ba399", BigDecimal.valueOf(100));
        PlannedContribution pc3 = new PlannedContribution("pc3", "ws1", "e732ab7b-9d12-40ce-b19b-330c36871f1f", BigDecimal.valueOf(100));

        ContributionEntry ce1 = new ContributionEntry("ce1", "ws1", "b9c08de4-a885-4c63-aa5b-d4be916e4ade", ContributionEntryType.DIRECT_EXPENSE, BigDecimal.valueOf(200), "Coffee", "e1");
        ContributionEntry ce2 = new ContributionEntry("ce2", "ws1", "b9c08de4-a885-4c63-aa5b-d4be916e4ade", ContributionEntryType.ADJUSTMENT, BigDecimal.valueOf(-200), "Coffee Adjustment", "e1");
        ContributionEntry ce3 = new ContributionEntry("ce3", "ws1", "b9c08de4-a885-4c63-aa5b-d4be916e4ade", ContributionEntryType.DIRECT_EXPENSE, BigDecimal.valueOf(100), "Coffee Updated", "e1");

        SplitAllocation sa1 = new SplitAllocation("sa1", "e1", "b9c08de4-a885-4c63-aa5b-d4be916e4ade", new Money(BigDecimal.valueOf(33.3333), "INR"), BigDecimal.valueOf(1));
        SplitAllocation sa2 = new SplitAllocation("sa2", "e1", "79f089e9-9bd3-4dd0-ac61-fa40435ba399", new Money(BigDecimal.valueOf(33.3333), "INR"), BigDecimal.valueOf(1));
        SplitAllocation sa3 = new SplitAllocation("sa3", "e1", "e732ab7b-9d12-40ce-b19b-330c36871f1f", new Money(BigDecimal.valueOf(33.3334), "INR"), BigDecimal.valueOf(1));

        WorkspaceFinancialState state = new WorkspaceFinancialState(
                "ws1",
                List.of(pc1, pc2, pc3),
                List.of(ce1, ce2, ce3),
                Collections.emptyList(),
                List.of(sa1, sa2, sa3),
                Collections.emptyList()
        );

        List<MemberBalance> balances = settlementEngine.calculateBalances(state, "INR");
        
        // Assert sum of balances remains zero
        BigDecimal sumBalances = balances.stream()
                .map(mb -> mb.balance().getAmount())
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        assertEquals(0, sumBalances.setScale(4).compareTo(BigDecimal.ZERO.setScale(4)));

        com.rkdevstudios.tripledger.settlement.domain.GreedySettlementOptimizer optimizer = new com.rkdevstudios.tripledger.settlement.domain.GreedySettlementOptimizer();
        List<com.rkdevstudios.tripledger.settlement.domain.SettlementTransfer> optimizedTransfers = optimizer.optimize(balances, "INR");

        assertEquals(2, optimizedTransfers.size());
        
        // Check transfers from debtors to the creditor payer
        assertTrue(optimizedTransfers.stream().anyMatch(t -> t.fromUserId().equals("79f089e9-9bd3-4dd0-ac61-fa40435ba399") 
                && t.toUserId().equals("b9c08de4-a885-4c63-aa5b-d4be916e4ade") && t.amount().getAmount().compareTo(BigDecimal.valueOf(33.3333)) == 0));
        assertTrue(optimizedTransfers.stream().anyMatch(t -> t.fromUserId().equals("e732ab7b-9d12-40ce-b19b-330c36871f1f") 
                && t.toUserId().equals("b9c08de4-a885-4c63-aa5b-d4be916e4ade") && t.amount().getAmount().compareTo(BigDecimal.valueOf(33.3334)) == 0));
    }
}
