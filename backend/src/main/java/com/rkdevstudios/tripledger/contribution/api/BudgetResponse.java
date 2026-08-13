package com.rkdevstudios.tripledger.contribution.api;

import java.math.BigDecimal;

public record BudgetResponse(
    BigDecimal totalBudget,
    BigDecimal totalSpent,
    BigDecimal remainingBudget
) {}
