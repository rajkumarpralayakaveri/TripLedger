package com.rkdevstudios.tripledger.contribution.api;

import com.rkdevstudios.tripledger.common.ApiResponse;
import com.rkdevstudios.tripledger.common.ApiRoutes;
import com.rkdevstudios.tripledger.contribution.application.PaymentProofService;
import com.rkdevstudios.tripledger.contribution.domain.PaymentProof;
import com.rkdevstudios.tripledger.contribution.domain.PaymentProofStatus;
import com.rkdevstudios.tripledger.identity.domain.User;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping(ApiRoutes.API_V1 + "/workspaces/{id}/payments")
public class PaymentProofController {

    private final PaymentProofService paymentProofService;

    public PaymentProofController(PaymentProofService paymentProofService) {
        this.paymentProofService = paymentProofService;
    }

    private User getAuthenticatedUser() {
        var authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !(authentication.getPrincipal() instanceof User)) {
            throw new SecurityException("User is not authenticated");
        }
        return (User) authentication.getPrincipal();
    }

    @PostMapping("/signature")
    public ResponseEntity<ApiResponse<PaymentSignatureResponse>> requestSignature(
            @PathVariable("id") String workspaceId,
            @Valid @RequestBody PaymentSignatureRequest request
    ) {
        User user = getAuthenticatedUser();
        Map<String, Object> signatureData = paymentProofService.createUploadRequest(
                workspaceId,
                user.getId(),
                request.amount()
        );

        PaymentSignatureResponse response = new PaymentSignatureResponse(
                (String) signatureData.get("paymentId"),
                (String) signatureData.get("publicId"),
                (String) signatureData.get("signature"),
                (Long) signatureData.get("timestamp"),
                (String) signatureData.get("apiKey"),
                (String) signatureData.get("cloudName")
        );

        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success(response));
    }

    @PostMapping("/complete")
    public ResponseEntity<ApiResponse<PaymentProofResponseDto>> completeUpload(
            @PathVariable("id") String workspaceId,
            @Valid @RequestBody PaymentCompletionRequest request
    ) {
        User user = getAuthenticatedUser();
        PaymentProof proof = paymentProofService.completeUpload(
                workspaceId,
                request.paymentId(),
                request.publicId(),
                user.getId()
        );

        PaymentProofResponseDto response = new PaymentProofResponseDto(
                proof.getId(),
                proof.getWorkspaceId(),
                proof.getUserId(),
                proof.getAmount(),
                proof.getStatus(),
                proof.getCreatedAt(),
                proof.getSubmittedAt(),
                proof.getVerifiedAt(),
                proof.getVerifiedBy(),
                proof.getRejectionReason(),
                null
        );

        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<PaymentProofResponseDto>>> listPayments(
            @PathVariable("id") String workspaceId
    ) {
        User user = getAuthenticatedUser();
        List<Map<String, Object>> proofsMap = paymentProofService.listProofs(workspaceId, user.getId());

        List<PaymentProofResponseDto> dtoList = new ArrayList<>();
        for (Map<String, Object> map : proofsMap) {
            dtoList.add(new PaymentProofResponseDto(
                    (String) map.get("id"),
                    (String) map.get("workspaceId"),
                    (String) map.get("userId"),
                    (BigDecimal) map.get("amount"),
                    (PaymentProofStatus) map.get("status"),
                    (Instant) map.get("createdAt"),
                    (Instant) map.get("submittedAt"),
                    (Instant) map.get("verifiedAt"),
                    (String) map.get("verifiedBy"),
                    (String) map.get("rejectionReason"),
                    (String) map.get("viewUrl")
            ));
        }

        return ResponseEntity.ok(ApiResponse.success(dtoList));
    }

    @PostMapping("/{paymentId}/approve")
    public ResponseEntity<ApiResponse<PaymentProofResponseDto>> approvePayment(
            @PathVariable("id") String workspaceId,
            @PathVariable("paymentId") String paymentId
    ) {
        User verifier = getAuthenticatedUser();
        PaymentProof proof = paymentProofService.approvePayment(workspaceId, paymentId, verifier.getId());

        PaymentProofResponseDto response = new PaymentProofResponseDto(
                proof.getId(),
                proof.getWorkspaceId(),
                proof.getUserId(),
                proof.getAmount(),
                proof.getStatus(),
                proof.getCreatedAt(),
                proof.getSubmittedAt(),
                proof.getVerifiedAt(),
                proof.getVerifiedBy(),
                proof.getRejectionReason(),
                null
        );

        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @PostMapping("/{paymentId}/reject")
    public ResponseEntity<ApiResponse<PaymentProofResponseDto>> rejectPayment(
            @PathVariable("id") String workspaceId,
            @PathVariable("paymentId") String paymentId,
            @Valid @RequestBody PaymentRejectionRequest request
    ) {
        User verifier = getAuthenticatedUser();
        PaymentProof proof = paymentProofService.rejectPayment(workspaceId, paymentId, request.reason(), verifier.getId());

        PaymentProofResponseDto response = new PaymentProofResponseDto(
                proof.getId(),
                proof.getWorkspaceId(),
                proof.getUserId(),
                proof.getAmount(),
                proof.getStatus(),
                proof.getCreatedAt(),
                proof.getSubmittedAt(),
                proof.getVerifiedAt(),
                proof.getVerifiedBy(),
                proof.getRejectionReason(),
                null
        );

        return ResponseEntity.ok(ApiResponse.success(response));
    }
}
