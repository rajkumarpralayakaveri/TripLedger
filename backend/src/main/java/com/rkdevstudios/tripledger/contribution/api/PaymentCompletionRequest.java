package com.rkdevstudios.tripledger.contribution.api;

import jakarta.validation.constraints.NotBlank;

public record PaymentCompletionRequest(
    @NotBlank(message = "Payment ID is required")
    String paymentId,
    @NotBlank(message = "Public ID is required")
    String publicId
) {}
