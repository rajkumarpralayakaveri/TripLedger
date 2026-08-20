package com.rkdevstudios.tripledger.contribution;

import com.rkdevstudios.tripledger.contribution.application.ContributionService;
import com.rkdevstudios.tripledger.contribution.application.PaymentProofService;
import com.rkdevstudios.tripledger.contribution.application.PaymentProofStorageService;
import com.rkdevstudios.tripledger.contribution.domain.*;
import com.rkdevstudios.tripledger.expense.domain.ActivityEntryRepository;
import com.rkdevstudios.tripledger.workspace.domain.MemberRole;
import com.rkdevstudios.tripledger.workspace.domain.WorkspaceMember;
import com.rkdevstudios.tripledger.workspace.domain.WorkspaceMemberRepository;
import com.rkdevstudios.tripledger.identity.domain.User;
import com.rkdevstudios.tripledger.identity.domain.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class PaymentProofServiceTest {

    private PaymentProofRepository paymentProofRepository;
    private PaymentProofStorageService storageService;
    private ContributionService contributionService;
    private WorkspaceMemberRepository workspaceMemberRepository;
    private ActivityEntryRepository activityEntryRepository;
    private UserRepository userRepository;
    private PaymentProofService paymentProofService;

    @BeforeEach
    void setUp() {
        paymentProofRepository = mock(PaymentProofRepository.class);
        storageService = mock(PaymentProofStorageService.class);
        contributionService = mock(ContributionService.class);
        workspaceMemberRepository = mock(WorkspaceMemberRepository.class);
        activityEntryRepository = mock(ActivityEntryRepository.class);
        userRepository = mock(UserRepository.class);
        
        when(paymentProofRepository.save(any(PaymentProof.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
                
        paymentProofService = new PaymentProofService(
                paymentProofRepository,
                storageService,
                contributionService,
                workspaceMemberRepository,
                activityEntryRepository,
                userRepository
        );
    }

    @Test
    void testNonMemberCannotSubmit() {
        String workspaceId = "ws_1";
        String userId = "usr_non_member";
        BigDecimal amount = BigDecimal.valueOf(5000);

        when(workspaceMemberRepository.findByWorkspaceIdAndUserId(workspaceId, userId))
                .thenReturn(Optional.empty());

        assertThrows(SecurityException.class, () ->
                paymentProofService.createUploadRequest(workspaceId, userId, amount)
        );
    }

    @Test
    void testMemberCannotApprove() {
        String workspaceId = "ws_1";
        String paymentId = "proof_1";
        String verifierId = "usr_member";

        WorkspaceMember verifier = new WorkspaceMember(workspaceId, verifierId, MemberRole.MEMBER);
        when(workspaceMemberRepository.findByWorkspaceIdAndUserId(workspaceId, verifierId))
                .thenReturn(Optional.of(verifier));

        assertThrows(SecurityException.class, () ->
                paymentProofService.approvePayment(workspaceId, paymentId, verifierId)
        );
    }

    @Test
    void testMemberCannotReject() {
        String workspaceId = "ws_1";
        String paymentId = "proof_1";
        String verifierId = "usr_member";

        WorkspaceMember verifier = new WorkspaceMember(workspaceId, verifierId, MemberRole.MEMBER);
        when(workspaceMemberRepository.findByWorkspaceIdAndUserId(workspaceId, verifierId))
                .thenReturn(Optional.of(verifier));

        assertThrows(SecurityException.class, () ->
                paymentProofService.rejectPayment(workspaceId, paymentId, "Rejection reason", verifierId)
        );
    }

    @Test
    void testMemberCannotViewOtherMemberProof() {
        String workspaceId = "ws_1";
        String callerId = "usr_member_1";
        String ownerId = "usr_member_2";

        WorkspaceMember caller = new WorkspaceMember(workspaceId, callerId, MemberRole.MEMBER);
        when(workspaceMemberRepository.findByWorkspaceIdAndUserId(workspaceId, callerId))
                .thenReturn(Optional.of(caller));

        PaymentProof proof = new PaymentProof("proof_1", workspaceId, ownerId, BigDecimal.valueOf(2000), "path_to_proof");
        when(paymentProofRepository.findByWorkspaceIdAndUserId(workspaceId, callerId))
                .thenReturn(Collections.emptyList());

        List<Map<String, Object>> result = paymentProofService.listProofs(workspaceId, callerId);
        assertTrue(result.isEmpty());
    }

    @Test
    void testAdminCanViewWorkspaceProofs() {
        String workspaceId = "ws_1";
        String adminId = "usr_admin";
        String memberId = "usr_member";

        WorkspaceMember admin = new WorkspaceMember(workspaceId, adminId, MemberRole.ADMIN);
        when(workspaceMemberRepository.findByWorkspaceIdAndUserId(workspaceId, adminId))
                .thenReturn(Optional.of(admin));

        PaymentProof proof = new PaymentProof("proof_1", workspaceId, memberId, BigDecimal.valueOf(2000), "path_to_proof");
        proof.setStatus(PaymentProofStatus.PENDING);
        when(paymentProofRepository.findByWorkspaceId(workspaceId))
                .thenReturn(Collections.singletonList(proof));
        when(storageService.generateSecureViewUrl("path_to_proof")).thenReturn("http://cloudinary.view.url");

        List<Map<String, Object>> result = paymentProofService.listProofs(workspaceId, adminId);
        assertEquals(1, result.size());
        assertEquals("http://cloudinary.view.url", result.get(0).get("viewUrl"));
    }

    @Test
    void testOwnerCanViewWorkspaceProofs() {
        String workspaceId = "ws_1";
        String ownerId = "usr_owner";
        String memberId = "usr_member";

        WorkspaceMember owner = new WorkspaceMember(workspaceId, ownerId, MemberRole.ADMIN);
        when(workspaceMemberRepository.findByWorkspaceIdAndUserId(workspaceId, ownerId))
                .thenReturn(Optional.of(owner));

        PaymentProof proof = new PaymentProof("proof_1", workspaceId, memberId, BigDecimal.valueOf(2000), "path_to_proof");
        proof.setStatus(PaymentProofStatus.PENDING);
        when(paymentProofRepository.findByWorkspaceId(workspaceId))
                .thenReturn(Collections.singletonList(proof));
        when(storageService.generateSecureViewUrl("path_to_proof")).thenReturn("http://cloudinary.view.url");

        List<Map<String, Object>> result = paymentProofService.listProofs(workspaceId, ownerId);
        assertEquals(1, result.size());
        assertEquals("http://cloudinary.view.url", result.get(0).get("viewUrl"));
    }

    @Test
    void testSelfApprovalBlocked() {
        String workspaceId = "ws_1";
        String verifierId = "usr_admin";

        WorkspaceMember verifier = new WorkspaceMember(workspaceId, verifierId, MemberRole.ADMIN);
        when(workspaceMemberRepository.findByWorkspaceIdAndUserId(workspaceId, verifierId))
                .thenReturn(Optional.of(verifier));

        PaymentProof proof = new PaymentProof("proof_1", workspaceId, verifierId, BigDecimal.valueOf(5000), "path_to_proof");
        proof.setStatus(PaymentProofStatus.PENDING);
        when(paymentProofRepository.findByIdWithLock("proof_1")).thenReturn(Optional.of(proof));

        assertThrows(SecurityException.class, () ->
                paymentProofService.approvePayment(workspaceId, "proof_1", verifierId)
        );
    }

    @Test
    void testCompletionIdempotency() {
        String workspaceId = "ws_1";
        String paymentId = "proof_1";
        String userId = "usr_member";

        WorkspaceMember member = new WorkspaceMember(workspaceId, userId, MemberRole.MEMBER);
        when(workspaceMemberRepository.findByWorkspaceIdAndUserId(workspaceId, userId))
                .thenReturn(Optional.of(member));

        PaymentProof proof = new PaymentProof(paymentId, workspaceId, userId, BigDecimal.valueOf(5000), "path_to_proof");
        proof.setStatus(PaymentProofStatus.APPROVED);
        when(paymentProofRepository.findByIdWithLock(paymentId)).thenReturn(Optional.of(proof));

        PaymentProof result = paymentProofService.completeUpload(workspaceId, paymentId, "path_to_proof", userId);
        assertEquals(PaymentProofStatus.APPROVED, result.getStatus());
        verifyNoInteractions(storageService);
    }

    @Test
    void testCloudinaryAssetValidation() {
        String workspaceId = "ws_1";
        String paymentId = "proof_1";
        String userId = "usr_member";

        WorkspaceMember member = new WorkspaceMember(workspaceId, userId, MemberRole.MEMBER);
        when(workspaceMemberRepository.findByWorkspaceIdAndUserId(workspaceId, userId))
                .thenReturn(Optional.of(member));

        PaymentProof proof = new PaymentProof(paymentId, workspaceId, userId, BigDecimal.valueOf(5000), "path_to_proof");
        proof.setStatus(PaymentProofStatus.UPLOAD_IN_PROGRESS);
        when(paymentProofRepository.findByIdWithLock(paymentId)).thenReturn(Optional.of(proof));

        // Mock invalid format
        CloudinaryAssetMetadata invalidFormatMeta = new CloudinaryAssetMetadata("path_to_proof", "image", "gif", 1000L);
        when(storageService.verifyUploadedAsset("path_to_proof")).thenReturn(invalidFormatMeta);

        assertThrows(IllegalArgumentException.class, () ->
                paymentProofService.completeUpload(workspaceId, paymentId, "path_to_proof", userId)
        );

        // Mock invalid size > 5MB
        CloudinaryAssetMetadata invalidSizeMeta = new CloudinaryAssetMetadata("path_to_proof", "image", "png", 6000000L);
        when(storageService.verifyUploadedAsset("path_to_proof")).thenReturn(invalidSizeMeta);

        assertThrows(IllegalArgumentException.class, () ->
                paymentProofService.completeUpload(workspaceId, paymentId, "path_to_proof", userId)
        );
    }

    @Test
    void testExpiredProofsCleanup() {
        Instant cutoff = Instant.now().minus(24, ChronoUnit.HOURS);
        PaymentProof proof1 = new PaymentProof("proof_1", "ws_1", "usr_1", BigDecimal.valueOf(1000), "path_1");
        proof1.setStatus(PaymentProofStatus.UPLOAD_IN_PROGRESS);

        when(paymentProofRepository.findByStatusAndCreatedAtBefore(eq(PaymentProofStatus.UPLOAD_IN_PROGRESS), any(Instant.class)))
                .thenReturn(Collections.singletonList(proof1));
        when(paymentProofRepository.findByIdWithLock("proof_1")).thenReturn(Optional.of(proof1));

        paymentProofService.cleanupExpiredUploads();

        verify(storageService).deleteAsset("path_1");
        verify(paymentProofRepository).delete(proof1);
    }

    @Test
    void testConcurrentApprovalIdempotency() {
        String workspaceId = "ws_1";
        String paymentId = "proof_1";
        String verifierId = "usr_admin";

        WorkspaceMember verifier = new WorkspaceMember(workspaceId, verifierId, MemberRole.ADMIN);
        when(workspaceMemberRepository.findByWorkspaceIdAndUserId(workspaceId, verifierId))
                .thenReturn(Optional.of(verifier));

        PaymentProof proof = new PaymentProof(paymentId, workspaceId, "usr_member", BigDecimal.valueOf(5000), "path_to_proof");
        proof.setStatus(PaymentProofStatus.PENDING);

        when(paymentProofRepository.findByIdWithLock(paymentId)).thenReturn(Optional.of(proof));

        PaymentProof approvedProof = paymentProofService.approvePayment(workspaceId, paymentId, verifierId);
        assertEquals(PaymentProofStatus.APPROVED, approvedProof.getStatus());
        verify(contributionService, times(1)).recordVerifiedCashContribution(any(), any(), any(), any(), any());

        proof.setStatus(PaymentProofStatus.APPROVED);
        PaymentProof secondApprovedProof = paymentProofService.approvePayment(workspaceId, paymentId, verifierId);
        
        assertEquals(PaymentProofStatus.APPROVED, secondApprovedProof.getStatus());
        verify(contributionService, times(1)).recordVerifiedCashContribution(any(), any(), any(), any(), any());
    }
}
