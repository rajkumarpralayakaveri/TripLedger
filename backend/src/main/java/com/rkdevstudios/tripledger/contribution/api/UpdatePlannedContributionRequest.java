package com.rkdevstudios.tripledger.contribution.api;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import java.math.BigDecimal;

public record UpdatePlannedContributionRequest(
    @NotNull(message = "Planned amount is required")
    @PositiveOrZero(message = "Planned amount must be non-negative")
    BigDecimal plannedAmount
) {}
