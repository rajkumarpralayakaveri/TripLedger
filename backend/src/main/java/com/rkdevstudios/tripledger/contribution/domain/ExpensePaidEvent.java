package com.rkdevstudios.tripledger.contribution.domain;

import java.math.BigDecimal;

public record ExpensePaidEvent(
    String workspaceId,
    String paidByUserId,
    BigDecimal amount,
    String description,
    String expenseId
) {}
