package com.rkdevstudios.tripledger.settlement.domain;

import com.rkdevstudios.tripledger.expense.domain.Money;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;

@Component
public class GreedySettlementOptimizer implements SettlementOptimizer {

    @Override
    public List<SettlementTransfer> optimize(List<MemberBalance> balances, String currency) {
        List<SettlementTransfer> transfers = new ArrayList<>();

        // 1. Separate debtors and creditors, ignoring net zero balances
        List<TempBalance> debtors = new ArrayList<>();
        List<TempBalance> creditors = new ArrayList<>();

        for (MemberBalance mb : balances) {
            BigDecimal bal = mb.balance().getAmount().setScale(4, RoundingMode.HALF_UP);
            int cmp = bal.compareTo(BigDecimal.ZERO);
            if (cmp < 0) {
                debtors.add(new TempBalance(mb.userId(), bal.abs()));
            } else if (cmp > 0) {
                creditors.add(new TempBalance(mb.userId(), bal));
            }
        }

        // 2. Loop and greedy match until balances are settled
        while (!debtors.isEmpty() && !creditors.isEmpty()) {
            // Sort deterministically to ensure order randomization has no effect on output plan
            debtors.sort(Comparator.comparing(TempBalance::amount).reversed().thenComparing(TempBalance::userId));
            creditors.sort(Comparator.comparing(TempBalance::amount).reversed().thenComparing(TempBalance::userId));

            TempBalance debtor = debtors.get(0);
            TempBalance creditor = creditors.get(0);

            BigDecimal transferAmount = debtor.amount.min(creditor.amount).setScale(4, RoundingMode.HALF_UP);

            if (transferAmount.compareTo(BigDecimal.ZERO) > 0) {
                // Generate a deterministic transfer ID for session tracking (e.g. fromUserId + toUserId + transferAmount hash)
                String transferId = UUID.nameUUIDFromBytes(
                        (debtor.userId + creditor.userId + transferAmount.toPlainString()).getBytes()
                ).toString();

                transfers.add(new SettlementTransfer(
                        transferId,
                        debtor.userId,
                        creditor.userId,
                        new Money(transferAmount, currency)
                ));

                debtor.amount = debtor.amount.subtract(transferAmount).setScale(4, RoundingMode.HALF_UP);
                creditor.amount = creditor.amount.subtract(transferAmount).setScale(4, RoundingMode.HALF_UP);
            }

            if (debtor.amount.compareTo(BigDecimal.ZERO) <= 0) {
                debtors.remove(0);
            }
            if (creditor.amount.compareTo(BigDecimal.ZERO) <= 0) {
                creditors.remove(0);
            }
        }

        return transfers;
    }

    private static class TempBalance {
        final String userId;
        BigDecimal amount;

        TempBalance(String userId, BigDecimal amount) {
            this.userId = userId;
            this.amount = amount;
        }

        BigDecimal amount() { return amount; }
        String userId() { return userId; }
    }
}
