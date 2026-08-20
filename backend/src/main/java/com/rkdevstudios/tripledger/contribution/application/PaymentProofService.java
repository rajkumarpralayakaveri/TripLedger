package com.rkdevstudios.tripledger.contribution.application;

import com.rkdevstudios.tripledger.contribution.domain.*;
import com.rkdevstudios.tripledger.expense.domain.ActivityEntry;
import com.rkdevstudios.tripledger.expense.domain.ActivityEntryRepository;
import com.rkdevstudios.tripledger.expense.domain.ActivityType;
import com.rkdevstudios.tripledger.identity.domain.User;
import com.rkdevstudios.tripledger.identity.domain.UserRepository;
import com.rkdevstudios.tripledger.workspace.domain.MemberRole;
import com.rkdevstudios.tripledger.workspace.domain.WorkspaceMember;
import com.rkdevstudios.tripledger.workspace.domain.WorkspaceMemberRepository;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.*;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Service
@Transactional(readOnly = true)
public class PaymentProofService {

    private static final Logger logger = LoggerFactory.getLogger(PaymentProofService.class);

    private final PaymentProofRepository paymentProofRepository;
    private final PaymentProofStorageService storageService;
    private final ContributionService contributionService;
    private final WorkspaceMemberRepository workspaceMemberRepository;
    private final ActivityEntryRepository activityEntryRepository;
    private final UserRepository userRepository;

    public PaymentProofService(
            PaymentProofRepository paymentProofRepository,
            PaymentProofStorageService storageService,
            ContributionService contributionService,
            WorkspaceMemberRepository workspaceMemberRepository,
            ActivityEntryRepository activityEntryRepository,
            UserRepository userRepository
    ) {
        this.paymentProofRepository = paymentProofRepository;
        this.storageService = storageService;
        this.contributionService = contributionService;
        this.workspaceMemberRepository = workspaceMemberRepository;
        this.activityEntryRepository = activityEntryRepository;
        this.userRepository = userRepository;
    }

    private void verifyMember(String workspaceId, String userId) {
        workspaceMemberRepository.findByWorkspaceIdAndUserId(workspaceId, userId)
                .orElseThrow(() -> new SecurityException("User is not a member of this workspace"));
    }

    private void verifyAuthorized(String workspaceId, String userId) {
        WorkspaceMember member = workspaceMemberRepository.findByWorkspaceIdAndUserId(workspaceId, userId)
                .orElseThrow(() -> new SecurityException("User is not a member of this workspace"));
        if (member.getRole() != MemberRole.ADMIN) {
            throw new SecurityException("Permission denied. ADMIN role required.");
        }
    }

    @Transactional
    public Map<String, Object> createUploadRequest(String workspaceId, String userId, BigDecimal amount) {
        verifyMember(workspaceId, userId);

        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Amount must be greater than zero");
        }

        String paymentId = UUID.randomUUID().toString();
        String publicId = "workspaces/" + workspaceId + "/proofs/" + paymentId;

        PaymentProof proof = new PaymentProof(paymentId, workspaceId, userId, amount, publicId);
        paymentProofRepository.save(proof);

        long timestamp = Instant.now().getEpochSecond();
        Map<String, Object> signatureData = storageService.generateUploadSignature(publicId, timestamp);
        Map<String, Object> result = new HashMap<>(signatureData);
        result.put("paymentId", paymentId);
        return result;
    }

    @Transactional
    public PaymentProof completeUpload(String workspaceId, String paymentId, String publicId, String userId) {
        long startTime = System.currentTimeMillis();
        logger.info("entered completeUpload - time: 0ms");

        verifyMember(workspaceId, userId);

        PaymentProof proof = paymentProofRepository.findByIdWithLock(paymentId)
                .orElseThrow(() -> new IllegalArgumentException("Payment proof not found"));
        logger.info("payment loaded - elapsed: {}ms", System.currentTimeMillis() - startTime);

        if (!proof.getWorkspaceId().equals(workspaceId)) {
            throw new IllegalArgumentException("Payment proof does not belong to this workspace");
        }

        if (!proof.getUserId().equals(userId)) {
            throw new SecurityException("Authenticated caller does not own this payment proof");
        }
        logger.info("authorization completed - elapsed: {}ms", System.currentTimeMillis() - startTime);

        if (proof.getStatus() != PaymentProofStatus.UPLOAD_IN_PROGRESS) {
            logger.info("payment already completed - status: {} - elapsed: {}ms", proof.getStatus(), System.currentTimeMillis() - startTime);
            return proof;
        }

        logger.info("Cloudinary verification started - elapsed: {}ms", System.currentTimeMillis() - startTime);
        CloudinaryAssetMetadata metadata = storageService.verifyUploadedAsset(publicId);
        logger.info("Cloudinary verification completed - elapsed: {}ms", System.currentTimeMillis() - startTime);

        if (metadata == null) {
            throw new IllegalStateException("Cloudinary asset verification returned empty metadata");
        }

        if (!metadata.publicId().equals(proof.getCloudinaryPublicId())) {
            throw new IllegalArgumentException("Cloudinary public ID does not match expected path");
        }

        if (!"image".equalsIgnoreCase(metadata.resourceType())) {
            throw new IllegalArgumentException("Invalid resource type. Receipt must be an image.");
        }

        String format = metadata.format();
        if (format == null || (!format.equalsIgnoreCase("jpg") && !format.equalsIgnoreCase("jpeg") && !format.equalsIgnoreCase("png") && !format.equalsIgnoreCase("webp"))) {
            throw new IllegalArgumentException("Unsupported image format. Allowed formats: JPEG, PNG, WebP.");
        }

        if (metadata.bytes() > 5242880L) { // 5MB limit
            throw new IllegalArgumentException("Receipt image size exceeds the maximum limit of 5MB.");
        }

        logger.info("payment update started - elapsed: {}ms", System.currentTimeMillis() - startTime);
        proof.setStatus(PaymentProofStatus.PENDING);
        proof.setSubmittedAt(Instant.now());
        PaymentProof saved = paymentProofRepository.save(proof);

        logger.info("activity persistence started - elapsed: {}ms", System.currentTimeMillis() - startTime);
        String meta = "{\"paymentId\":\"" + saved.getId() + "\",\"amount\":\"" + saved.getAmount() + "\"}";
        ActivityEntry entry = new ActivityEntry(
                UUID.randomUUID().toString(),
                workspaceId,
                saved.getUserId(),
                ActivityType.PAYMENT_SUBMITTED,
                meta
        );
        activityEntryRepository.save(entry);

        logger.info("transaction completed - elapsed: {}ms", System.currentTimeMillis() - startTime);
        return saved;
    }

    public List<Map<String, Object>> listProofs(String workspaceId, String callerUserId) {
        verifyMember(workspaceId, callerUserId);

        WorkspaceMember caller = workspaceMemberRepository.findByWorkspaceIdAndUserId(workspaceId, callerUserId)
                .orElseThrow(() -> new SecurityException("Caller membership not found"));

        List<PaymentProof> proofs;
        if (caller.getRole() == MemberRole.ADMIN) {
            proofs = paymentProofRepository.findByWorkspaceId(workspaceId);
        } else {
            proofs = paymentProofRepository.findByWorkspaceIdAndUserId(workspaceId, callerUserId);
        }

        List<Map<String, Object>> result = new ArrayList<>();
        for (PaymentProof p : proofs) {
            Map<String, Object> map = new HashMap<>();
            map.put("id", p.getId());
            map.put("workspaceId", p.getWorkspaceId());
            map.put("userId", p.getUserId());
            String payerName = userRepository.findById(p.getUserId())
                    .map(User::getName)
                    .orElse("Someone");
            map.put("payerName", payerName);
            map.put("amount", p.getAmount());
            map.put("status", p.getStatus());
            map.put("createdAt", p.getCreatedAt());
            map.put("submittedAt", p.getSubmittedAt());
            map.put("verifiedAt", p.getVerifiedAt());
            map.put("verifiedBy", p.getVerifiedBy());
            map.put("rejectionReason", p.getRejectionReason());

            if (p.getStatus() == PaymentProofStatus.PENDING || p.getStatus() == PaymentProofStatus.APPROVED || p.getStatus() == PaymentProofStatus.REJECTED) {
                map.put("viewUrl", storageService.generateSecureViewUrl(p.getCloudinaryPublicId()));
            }
            result.add(map);
        }
        return result;
    }

    @Transactional
    public PaymentProof approvePayment(String workspaceId, String paymentId, String verifierId) {
        verifyAuthorized(workspaceId, verifierId);

        PaymentProof proof = paymentProofRepository.findByIdWithLock(paymentId)
                .orElseThrow(() -> new IllegalArgumentException("Payment proof not found"));

        if (!proof.getWorkspaceId().equals(workspaceId)) {
            throw new IllegalArgumentException("Payment proof does not belong to this workspace");
        }

        if (proof.getStatus() == PaymentProofStatus.APPROVED) {
            return proof;
        }

        if (proof.getStatus() != PaymentProofStatus.PENDING) {
            throw new IllegalStateException("Payment proof is not in PENDING state");
        }

        if (verifierId.equals(proof.getUserId())) {
            throw new SecurityException("Self-approval is forbidden. An admin cannot approve their own payment.");
        }

        contributionService.recordVerifiedCashContribution(
                workspaceId,
                proof.getUserId(),
                proof.getAmount(),
                proof.getId(),
                verifierId
        );

        proof.setStatus(PaymentProofStatus.APPROVED);
        proof.setVerifiedBy(verifierId);
        proof.setVerifiedAt(Instant.now());
        PaymentProof saved = paymentProofRepository.save(proof);

        String meta = "{\"paymentId\":\"" + saved.getId() + "\",\"amount\":\"" + saved.getAmount() + "\"}";
        ActivityEntry entry = new ActivityEntry(
                UUID.randomUUID().toString(),
                workspaceId,
                saved.getUserId(),
                ActivityType.PAYMENT_APPROVED,
                meta
        );
        activityEntryRepository.save(entry);

        return saved;
    }

    @Transactional
    public PaymentProof rejectPayment(String workspaceId, String paymentId, String reason, String verifierId) {
        verifyAuthorized(workspaceId, verifierId);

        if (reason == null || reason.trim().isEmpty()) {
            throw new IllegalArgumentException("Rejection reason is required");
        }

        PaymentProof proof = paymentProofRepository.findByIdWithLock(paymentId)
                .orElseThrow(() -> new IllegalArgumentException("Payment proof not found"));

        if (!proof.getWorkspaceId().equals(workspaceId)) {
            throw new IllegalArgumentException("Payment proof does not belong to this workspace");
        }

        if (proof.getStatus() == PaymentProofStatus.REJECTED) {
            return proof;
        }

        if (proof.getStatus() != PaymentProofStatus.PENDING) {
            throw new IllegalStateException("Payment proof is not in PENDING state");
        }

        if (verifierId.equals(proof.getUserId())) {
            throw new SecurityException("Self-rejection is forbidden. An admin cannot verify their own payment.");
        }

        proof.setStatus(PaymentProofStatus.REJECTED);
        proof.setRejectionReason(reason);
        proof.setVerifiedBy(verifierId);
        proof.setVerifiedAt(Instant.now());
        PaymentProof saved = paymentProofRepository.save(proof);

        String meta = "{\"paymentId\":\"" + saved.getId() + "\",\"reason\":\"" + reason + "\"}";
        ActivityEntry entry = new ActivityEntry(
                UUID.randomUUID().toString(),
                workspaceId,
                saved.getUserId(),
                ActivityType.PAYMENT_REJECTED,
                meta
        );
        activityEntryRepository.save(entry);

        return saved;
    }

    @Scheduled(cron = "0 0 2 * * ?") // Nightly at 2:00 AM
    @Transactional
    public void cleanupExpiredUploads() {
        Instant cutoff = Instant.now().minus(24, ChronoUnit.HOURS);
        List<PaymentProof> expiredProofs = paymentProofRepository.findByStatusAndCreatedAtBefore(PaymentProofStatus.UPLOAD_IN_PROGRESS, cutoff);
        for (PaymentProof p : expiredProofs) {
            paymentProofRepository.findByIdWithLock(p.getId()).ifPresent(lockedProof -> {
                if (lockedProof.getStatus() == PaymentProofStatus.UPLOAD_IN_PROGRESS) {
                    try {
                        storageService.deleteAsset(lockedProof.getCloudinaryPublicId());
                    } catch (Exception e) {
                        // ignore and clean row
                    }
                    paymentProofRepository.delete(lockedProof);
                }
            });
        }
    }
}
