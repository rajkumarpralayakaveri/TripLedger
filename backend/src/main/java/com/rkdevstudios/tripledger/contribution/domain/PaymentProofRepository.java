package com.rkdevstudios.tripledger.contribution.domain;

import java.util.List;
import java.util.Optional;

public interface PaymentProofRepository {
    PaymentProof save(PaymentProof proof);
    Optional<PaymentProof> findById(String id);
    Optional<PaymentProof> findByIdWithLock(String id);
    List<PaymentProof> findByWorkspaceId(String workspaceId);
    List<PaymentProof> findByWorkspaceIdAndUserId(String workspaceId, String userId);
    List<PaymentProof> findByStatusAndCreatedAtBefore(PaymentProofStatus status, java.time.Instant cutoff);
    void delete(PaymentProof proof);
}
