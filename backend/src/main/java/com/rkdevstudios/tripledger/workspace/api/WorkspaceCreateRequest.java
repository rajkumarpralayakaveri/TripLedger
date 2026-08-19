package com.rkdevstudios.tripledger.workspace.api;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.LocalDate;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Pattern;
import com.rkdevstudios.tripledger.contribution.domain.ContributionStrategy;
import java.util.Map;

public record WorkspaceCreateRequest(
    @NotBlank(message = "Name is required")
    @Size(max = 100, message = "Name must not exceed 100 characters")
    String name,

    String description,

    @NotNull(message = "Start date is required")
    LocalDate startDate,

    @NotNull(message = "End date is required")
    LocalDate endDate,

    @NotBlank(message = "Base currency is required")
    @Size(min = 3, max = 3, message = "Base currency must be a 3-letter ISO code")
    @Pattern(regexp = "^(INR|USD|EUR|GBP)$", message = "Currency must be one of: INR, USD, EUR, GBP")
    String baseCurrency,

    @Positive(message = "Budget must be positive")
    BigDecimal budget,

    @NotNull(message = "Planned member count is required")
    @Min(value = 1, message = "Planned member count must be at least 1")
    Integer plannedMemberCount,

    ContributionStrategy contributionStrategy,
    Map<String, BigDecimal> customAmounts,
    Map<String, BigDecimal> percentages
) {}
