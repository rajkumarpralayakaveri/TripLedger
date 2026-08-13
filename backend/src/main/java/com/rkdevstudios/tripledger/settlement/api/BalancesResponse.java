package com.rkdevstudios.tripledger.settlement.api;

import com.rkdevstudios.tripledger.expense.domain.Money;
import java.util.List;

public record BalancesResponse(
    List<MemberBalanceResponse> balances
) {
    public record MemberBalanceResponse(
        String userId,
        String userName,
        Money paid,
        Money owed,
        Money balance
    ) {}
}
