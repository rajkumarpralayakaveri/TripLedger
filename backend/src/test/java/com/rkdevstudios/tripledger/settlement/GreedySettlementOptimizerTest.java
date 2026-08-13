package com.rkdevstudios.tripledger.settlement;

import com.rkdevstudios.tripledger.expense.domain.Money;
import com.rkdevstudios.tripledger.settlement.domain.GreedySettlementOptimizer;
import com.rkdevstudios.tripledger.settlement.domain.MemberBalance;
import com.rkdevstudios.tripledger.settlement.domain.SettlementTransfer;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class GreedySettlementOptimizerTest {

    private final GreedySettlementOptimizer optimizer = new GreedySettlementOptimizer();

    @Test
    public void testOptimizeDeterminismAndCorrectness() {
        // Raj: +5000, Amit: -3000, Kamal: -2000
        MemberBalance b1 = new MemberBalance("Raj", new Money(BigDecimal.valueOf(15000), "INR"), new Money(BigDecimal.valueOf(10000), "INR"), new Money(BigDecimal.valueOf(5000), "INR"));
        MemberBalance b2 = new MemberBalance("Amit", new Money(BigDecimal.valueOf(7000), "INR"), new Money(BigDecimal.valueOf(10000), "INR"), new Money(BigDecimal.valueOf(-3000), "INR"));
        MemberBalance b3 = new MemberBalance("Kamal", new Money(BigDecimal.valueOf(8000), "INR"), new Money(BigDecimal.valueOf(10000), "INR"), new Money(BigDecimal.valueOf(-2000), "INR"));

        List<MemberBalance> list1 = List.of(b1, b2, b3);

        List<SettlementTransfer> plan1 = optimizer.optimize(list1, "INR");

        assertEquals(2, plan1.size());

        // Verifying order does not affect plan output (Determinism Invariant)
        List<MemberBalance> list2 = new ArrayList<>(list1);
        Collections.shuffle(list2);
        List<SettlementTransfer> plan2 = optimizer.optimize(list2, "INR");

        assertEquals(plan1.size(), plan2.size());
        for (int i = 0; i < plan1.size(); i++) {
            assertEquals(plan1.get(i).fromUserId(), plan2.get(i).fromUserId());
            assertEquals(plan1.get(i).toUserId(), plan2.get(i).toUserId());
            assertEquals(plan1.get(i).amount().getAmount(), plan2.get(i).amount().getAmount());
        }
    }
}
