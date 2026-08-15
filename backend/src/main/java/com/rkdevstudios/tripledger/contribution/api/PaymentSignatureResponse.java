package com.rkdevstudios.tripledger.contribution.api;

public record PaymentSignatureResponse(
    String paymentId,
    String publicId,
    String signature,
    long timestamp,
    String apiKey,
    String cloudName
) {}
