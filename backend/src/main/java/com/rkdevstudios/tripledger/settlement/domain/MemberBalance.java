package com.rkdevstudios.tripledger.settlement.domain;

import com.rkdevstudios.tripledger.expense.domain.Money;

public record MemberBalance(
    String userId,
    Money paid,
    Money owed,
    Money balance
) {}
