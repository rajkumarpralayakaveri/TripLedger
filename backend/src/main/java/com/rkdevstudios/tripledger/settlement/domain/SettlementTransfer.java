package com.rkdevstudios.tripledger.settlement.domain;

import com.rkdevstudios.tripledger.expense.domain.Money;

public record SettlementTransfer(
    String id,
    String fromUserId,
    String toUserId,
    Money amount
) {}
