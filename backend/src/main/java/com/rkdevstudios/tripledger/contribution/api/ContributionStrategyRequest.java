package com.rkdevstudios.tripledger.contribution.api;

import com.rkdevstudios.tripledger.contribution.domain.ContributionStrategy;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.util.Map;

public record ContributionStrategyRequest(
    @NotNull(message = "Strategy is required")
    ContributionStrategy strategy,

    Map<String, BigDecimal> customAmounts,
    Map<String, BigDecimal> percentages
) {}
