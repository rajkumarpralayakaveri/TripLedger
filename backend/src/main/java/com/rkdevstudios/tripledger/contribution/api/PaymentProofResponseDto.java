package com.rkdevstudios.tripledger.contribution.api;

import com.rkdevstudios.tripledger.contribution.domain.PaymentProofStatus;
import java.math.BigDecimal;
import java.time.Instant;

public record PaymentProofResponseDto(
    String id,
    String workspaceId,
    String userId,
    String payerName,
    BigDecimal amount,
    PaymentProofStatus status,
    Instant createdAt,
    Instant submittedAt,
    Instant verifiedAt,
    String verifiedBy,
    String rejectionReason,
    String viewUrl
) {}
