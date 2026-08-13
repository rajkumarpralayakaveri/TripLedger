package com.rkdevstudios.tripledger.expense;

import com.rkdevstudios.tripledger.expense.application.SplitCalculationService;
import com.rkdevstudios.tripledger.expense.domain.SplitType;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

class SplitCalculationServiceTest {

    private final SplitCalculationService calculationService = new SplitCalculationService();

    @Test
    void testEqualSplit() {
        BigDecimal total = BigDecimal.valueOf(100);
        List<String> participants = Arrays.asList("usr_1", "usr_2", "usr_3");

        Map<String, BigDecimal> splits = calculationService.calculateSplits(total, SplitType.EQUAL, participants, null);

        assertEquals(3, splits.size());
        // 100 / 3 = 33.3333 with the remainder given to the last one (33.3334)
        assertEquals(0, splits.get("usr_1").compareTo(BigDecimal.valueOf(33.3333)));
        assertEquals(0, splits.get("usr_2").compareTo(BigDecimal.valueOf(33.3333)));
        assertEquals(0, splits.get("usr_3").compareTo(BigDecimal.valueOf(33.3334)));
    }

    @Test
    void testExactSplit_Success() {
        BigDecimal total = BigDecimal.valueOf(1500);
        List<String> participants = Arrays.asList("usr_1", "usr_2");
        Map<String, BigDecimal> values = new HashMap<>();
        values.put("usr_1", BigDecimal.valueOf(1000));
        values.put("usr_2", BigDecimal.valueOf(500));

        Map<String, BigDecimal> splits = calculationService.calculateSplits(total, SplitType.EXACT, participants, values);

        assertEquals(0, splits.get("usr_1").compareTo(BigDecimal.valueOf(1000)));
        assertEquals(0, splits.get("usr_2").compareTo(BigDecimal.valueOf(500)));
    }

    @Test
    void testExactSplit_MismatchedTotal() {
        BigDecimal total = BigDecimal.valueOf(1500);
        List<String> participants = Arrays.asList("usr_1", "usr_2");
        Map<String, BigDecimal> values = new HashMap<>();
        values.put("usr_1", BigDecimal.valueOf(900));
        values.put("usr_2", BigDecimal.valueOf(500));

        assertThrows(IllegalArgumentException.class, () -> {
            calculationService.calculateSplits(total, SplitType.EXACT, participants, values);
        });
    }

    @Test
    void testPercentageSplit() {
        BigDecimal total = BigDecimal.valueOf(2500);
        List<String> participants = Arrays.asList("usr_1", "usr_2");
        Map<String, BigDecimal> values = new HashMap<>();
        values.put("usr_1", BigDecimal.valueOf(60));
        values.put("usr_2", BigDecimal.valueOf(40));

        Map<String, BigDecimal> splits = calculationService.calculateSplits(total, SplitType.PERCENTAGE, participants, values);

        assertEquals(0, splits.get("usr_1").compareTo(BigDecimal.valueOf(1500)));
        assertEquals(0, splits.get("usr_2").compareTo(BigDecimal.valueOf(1000)));
    }

    @Test
    void testSharesSplit() {
        BigDecimal total = BigDecimal.valueOf(3000);
        List<String> participants = Arrays.asList("usr_1", "usr_2", "usr_3");
        Map<String, BigDecimal> values = new HashMap<>();
        values.put("usr_1", BigDecimal.valueOf(1)); // 1 share
        values.put("usr_2", BigDecimal.valueOf(2)); // 2 shares
        values.put("usr_3", BigDecimal.valueOf(3)); // 3 shares
        // Total shares = 6.usr_1=500, usr_2=1000, usr_3=1500

        Map<String, BigDecimal> splits = calculationService.calculateSplits(total, SplitType.SHARES, participants, values);

        assertEquals(0, splits.get("usr_1").compareTo(BigDecimal.valueOf(500)));
        assertEquals(0, splits.get("usr_2").compareTo(BigDecimal.valueOf(1000)));
        assertEquals(0, splits.get("usr_3").compareTo(BigDecimal.valueOf(1500)));
    }
}
