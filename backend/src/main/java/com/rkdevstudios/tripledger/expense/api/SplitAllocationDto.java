package com.rkdevstudios.tripledger.expense.api;

import java.math.BigDecimal;

public record SplitAllocationDto(
    String userId,
    String name,
    BigDecimal amount,
    String currency,
    BigDecimal rawValue
) {}
