package com.rkdevstudios.tripledger.contribution.api;

import jakarta.validation.constraints.NotBlank;

public record PaymentRejectionRequest(
    @NotBlank(message = "Rejection reason is required")
    String reason
) {}
