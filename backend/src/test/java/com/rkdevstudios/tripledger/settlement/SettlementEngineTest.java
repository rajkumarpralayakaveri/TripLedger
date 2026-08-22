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
    public void testCalculateBalances_AfterExpenseDeleted_InvariantRemainsZero() {
        // Setup planned users
        PlannedContribution pc1 = new PlannedContribution("pc1", "ws1", "Raj", BigDecimal.valueOf(10000));
        PlannedContribution pc2 = new PlannedContribution("pc2", "ws1", "Amit", BigDecimal.valueOf(10000));

        // Legacy active expense direct contribution entry
        ContributionEntry activeCe = new ContributionEntry("ce1", "ws1", "Raj", ContributionEntryType.DIRECT_EXPENSE, BigDecimal.valueOf(1000), "Lunch", "e1");
        SplitAllocation activeSa1 = new SplitAllocation("sa1", "e1", "Raj", new Money(BigDecimal.valueOf(500), "INR"), BigDecimal.valueOf(1));
        SplitAllocation activeSa2 = new SplitAllocation("sa2", "e1", "Amit", new Money(BigDecimal.valueOf(500), "INR"), BigDecimal.valueOf(1));

        // Deleted expense entries: payer was credited 400, then adjusted -400. Allocations are removed.
        ContributionEntry deletedCe = new ContributionEntry("ce2", "ws1", "Raj", ContributionEntryType.DIRECT_EXPENSE, BigDecimal.valueOf(400), "Deleted Taxi", "e2");
        ContributionEntry deletedAdj = new ContributionEntry("ce3", "ws1", "Raj", ContributionEntryType.ADJUSTMENT, BigDecimal.valueOf(-400), "Deleted Taxi Adjustment", "e2");

        WorkspaceFinancialState state = new WorkspaceFinancialState(
                "ws1",
                List.of(pc1, pc2),
                List.of(activeCe, deletedCe, deletedAdj),
                Collections.emptyList(),
                List.of(activeSa1, activeSa2), // allocations for deleted expense e2 are correctly missing
                Collections.emptyList()
        );

        // This calculateBalances call should succeed with sumBalances = 0 without invariant exceptions
        List<MemberBalance> balances = settlementEngine.calculateBalances(state, "INR");
        BigDecimal sumBalances = balances.stream()
                .map(mb -> mb.balance().getAmount())
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        assertEquals(0, sumBalances.setScale(4).compareTo(BigDecimal.ZERO.setScale(4)));
    }
}
