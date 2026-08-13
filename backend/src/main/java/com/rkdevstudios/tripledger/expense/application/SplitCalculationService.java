package com.rkdevstudios.tripledger.expense.application;

import com.rkdevstudios.tripledger.expense.domain.SplitType;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class SplitCalculationService {

    public Map<String, BigDecimal> calculateSplits(
            BigDecimal totalAmount,
            SplitType splitType,
            List<String> participantIds,
            Map<String, BigDecimal> values // represents amounts, percentages, or share weights
    ) {
        if (participantIds == null || participantIds.isEmpty()) {
            throw new IllegalArgumentException("Participant list must contain at least one user");
        }
        if (totalAmount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Expense amount must be greater than zero");
        }

        Map<String, BigDecimal> allocations = new HashMap<>();

        switch (splitType) {
            case EQUAL -> {
                BigDecimal size = BigDecimal.valueOf(participantIds.size());
                BigDecimal equalShare = totalAmount.divide(size, 4, RoundingMode.HALF_UP);
                
                // Track remainder to ensure sum equals total exactly
                BigDecimal runningSum = BigDecimal.ZERO;
                for (int i = 0; i < participantIds.size() - 1; i++) {
                    allocations.put(participantIds.get(i), equalShare);
                    runningSum = runningSum.add(equalShare);
                }
                // Assign remainder to last participant
                BigDecimal remainderShare = totalAmount.subtract(runningSum);
                allocations.put(participantIds.get(participantIds.size() - 1), remainderShare);
            }
            case EXACT -> {
                BigDecimal sum = BigDecimal.ZERO;
                for (String userId : participantIds) {
                    BigDecimal val = values.getOrDefault(userId, BigDecimal.ZERO);
                    if (val.compareTo(BigDecimal.ZERO) < 0) {
                        throw new IllegalArgumentException("Split amount cannot be negative");
                    }
                    allocations.put(userId, val.setScale(4, RoundingMode.HALF_UP));
                    sum = sum.add(val);
                }
                if (sum.compareTo(totalAmount) != 0) {
                    throw new IllegalArgumentException("Sum of split amounts (" + sum + ") must equal total amount (" + totalAmount + ")");
                }
            }
            case PERCENTAGE -> {
                BigDecimal totalPercent = BigDecimal.ZERO;
                for (String userId : participantIds) {
                    BigDecimal percent = values.getOrDefault(userId, BigDecimal.ZERO);
                    if (percent.compareTo(BigDecimal.ZERO) < 0) {
                        throw new IllegalArgumentException("Split percentage cannot be negative");
                    }
                    totalPercent = totalPercent.add(percent);
                }
                if (totalPercent.compareTo(BigDecimal.valueOf(100)) != 0) {
                    throw new IllegalArgumentException("Sum of percentages (" + totalPercent + ") must equal 100%");
                }

                BigDecimal runningSum = BigDecimal.ZERO;
                for (int i = 0; i < participantIds.size() - 1; i++) {
                    String userId = participantIds.get(i);
                    BigDecimal percent = values.getOrDefault(userId, BigDecimal.ZERO);
                    BigDecimal share = totalAmount.multiply(percent).divide(BigDecimal.valueOf(100), 4, RoundingMode.HALF_UP);
                    allocations.put(userId, share);
                    runningSum = runningSum.add(share);
                }
                // Last participant receives remainder to eliminate precision gaps
                String lastUserId = participantIds.get(participantIds.size() - 1);
                allocations.put(lastUserId, totalAmount.subtract(runningSum));
            }
            case SHARES -> {
                BigDecimal totalShares = BigDecimal.ZERO;
                for (String userId : participantIds) {
                    BigDecimal shares = values.getOrDefault(userId, BigDecimal.ZERO);
                    if (shares.compareTo(BigDecimal.ZERO) < 0) {
                        throw new IllegalArgumentException("Split shares cannot be negative");
                    }
                    totalShares = totalShares.add(shares);
                }
                if (totalShares.compareTo(BigDecimal.ZERO) <= 0) {
                    throw new IllegalArgumentException("Sum of shares must be greater than zero");
                }

                BigDecimal runningSum = BigDecimal.ZERO;
                for (int i = 0; i < participantIds.size() - 1; i++) {
                    String userId = participantIds.get(i);
                    BigDecimal shares = values.getOrDefault(userId, BigDecimal.ZERO);
                    BigDecimal share = totalAmount.multiply(shares).divide(totalShares, 4, RoundingMode.HALF_UP);
                    allocations.put(userId, share);
                    runningSum = runningSum.add(share);
                }
                String lastUserId = participantIds.get(participantIds.size() - 1);
                allocations.put(lastUserId, totalAmount.subtract(runningSum));
            }
        }

        return allocations;
    }
}
