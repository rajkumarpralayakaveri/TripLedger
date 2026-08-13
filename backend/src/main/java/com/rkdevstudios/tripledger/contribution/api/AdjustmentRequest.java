package com.rkdevstudios.tripledger.contribution.api;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;

public record AdjustmentRequest(
    @NotBlank(message = "User ID is required")
    String userId,

    @NotNull(message = "Amount is required")
    BigDecimal amount,

    @NotBlank(message = "Reason/description is required")
    String reason
) {}
