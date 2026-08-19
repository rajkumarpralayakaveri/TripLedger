package com.rkdevstudios.tripledger.contribution.api;

import com.rkdevstudios.tripledger.contribution.application.PaymentProofStorageService;
import com.rkdevstudios.tripledger.common.api.GlobalExceptionHandler;
import com.rkdevstudios.tripledger.contribution.application.PaymentProofService;
import com.rkdevstudios.tripledger.contribution.domain.PaymentProof;
import com.rkdevstudios.tripledger.contribution.domain.PaymentProofStatus;
import com.rkdevstudios.tripledger.identity.domain.User;
import com.rkdevstudios.tripledger.identity.domain.UserRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.http.MediaType;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.math.BigDecimal;
import java.util.*;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class PaymentProofControllerTest {

    private MockMvc mockMvc;
    private PaymentProofService paymentProofService;
    private UserRepository userRepository;
    private PaymentProofStorageService storageService;
    private SecurityContext originalSecurityContext;

    @BeforeEach
    void setUp() {
        paymentProofService = mock(PaymentProofService.class);
        userRepository = mock(UserRepository.class);
        storageService = mock(PaymentProofStorageService.class);
        
        User mockUser = new User("usr_1", "Raj", "raj@example.com", "hashed_password", null);
        when(userRepository.findById("usr_1")).thenReturn(Optional.of(mockUser));
        when(userRepository.findById("usr_2")).thenReturn(Optional.of(new User("usr_2", "John", "john@example.com", "pass", null)));

        PaymentProofController controller = new PaymentProofController(paymentProofService, userRepository, storageService);

        mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();

        originalSecurityContext = SecurityContextHolder.getContext();
        SecurityContext mockContext = mock(SecurityContext.class);
        Authentication mockAuthentication = mock(Authentication.class);
        
        when(mockAuthentication.getPrincipal()).thenReturn(mockUser);
        when(mockContext.getAuthentication()).thenReturn(mockAuthentication);
        SecurityContextHolder.setContext(mockContext);
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.setContext(originalSecurityContext);
    }

    @Test
    void testRequestSignatureReturnsCreated() throws Exception {
        String workspaceId = "ws_1";
        Map<String, Object> mockSig = new HashMap<>();
        mockSig.put("paymentId", "proof_1");
        mockSig.put("publicId", "workspaces/ws_1/proofs/proof_1");
        mockSig.put("signature", "cloudinary_sig_123");
        mockSig.put("timestamp", 1786629628L);
        mockSig.put("apiKey", "api_key_val");
        mockSig.put("cloudName", "cloudinary_cloud");

        when(paymentProofService.createUploadRequest(eq(workspaceId), eq("usr_1"), any(BigDecimal.class)))
                .thenReturn(mockSig);

        mockMvc.perform(post("/api/v1/workspaces/{id}/payments/signature", workspaceId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"amount\":7000.0}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.paymentId").value("proof_1"))
                .andExpect(jsonPath("$.data.signature").value("cloudinary_sig_123"));
    }

    @Test
    void testCompleteUploadReturnsOk() throws Exception {
        String workspaceId = "ws_1";
        PaymentProof proof = new PaymentProof("proof_1", workspaceId, "usr_1", BigDecimal.valueOf(7000), "public_id_path");
        proof.setStatus(PaymentProofStatus.PENDING);

        when(paymentProofService.completeUpload(workspaceId, "proof_1", "public_id_path", "usr_1"))
                .thenReturn(proof);

        mockMvc.perform(post("/api/v1/workspaces/{id}/payments/complete", workspaceId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"paymentId\":\"proof_1\",\"publicId\":\"public_id_path\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.status").value("PENDING"));
    }

    @Test
    void testListPaymentsReturnsOk() throws Exception {
        String workspaceId = "ws_1";
        Map<String, Object> map = new HashMap<>();
        map.put("id", "proof_1");
        map.put("workspaceId", workspaceId);
        map.put("userId", "usr_1");
        map.put("amount", BigDecimal.valueOf(7000));
        map.put("status", PaymentProofStatus.PENDING);
        map.put("viewUrl", "http://cloudinary.secure.view.url");

        when(paymentProofService.listProofs(workspaceId, "usr_1"))
                .thenReturn(Collections.singletonList(map));

        mockMvc.perform(get("/api/v1/workspaces/{id}/payments", workspaceId)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data[0].id").value("proof_1"))
                .andExpect(jsonPath("$.data[0].viewUrl").value("http://cloudinary.secure.view.url"));
    }

    @Test
    void testApprovePaymentReturnsOk() throws Exception {
        String workspaceId = "ws_1";
        PaymentProof proof = new PaymentProof("proof_1", workspaceId, "usr_2", BigDecimal.valueOf(7000), "public_id_path");
        proof.setStatus(PaymentProofStatus.APPROVED);
        proof.setVerifiedBy("usr_1");

        when(paymentProofService.approvePayment(workspaceId, "proof_1", "usr_1"))
                .thenReturn(proof);

        mockMvc.perform(post("/api/v1/workspaces/{id}/payments/{paymentId}/approve", workspaceId, "proof_1")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.status").value("APPROVED"))
                .andExpect(jsonPath("$.data.verifiedBy").value("usr_1"));
    }

    @Test
    void testRejectPaymentReturnsOk() throws Exception {
        String workspaceId = "ws_1";
        PaymentProof proof = new PaymentProof("proof_1", workspaceId, "usr_2", BigDecimal.valueOf(7000), "public_id_path");
        proof.setStatus(PaymentProofStatus.REJECTED);
        proof.setRejectionReason("Mismatched receipt amount");
        proof.setVerifiedBy("usr_1");

        when(paymentProofService.rejectPayment(workspaceId, "proof_1", "Mismatched receipt amount", "usr_1"))
                .thenReturn(proof);

        mockMvc.perform(post("/api/v1/workspaces/{id}/payments/{paymentId}/reject", workspaceId, "proof_1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"reason\":\"Mismatched receipt amount\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.status").value("REJECTED"))
                .andExpect(jsonPath("$.data.rejectionReason").value("Mismatched receipt amount"));
    }

    @Test
    void testCompleteUploadVerificationTimeoutReturns500() throws Exception {
        String workspaceId = "ws_1";
        when(paymentProofService.completeUpload(eq(workspaceId), eq("proof_1"), eq("public_id_path"), eq("usr_1")))
                .thenThrow(new RuntimeException("Cloudinary asset verification failed: connect timed out"));

        mockMvc.perform(post("/api/v1/workspaces/{id}/payments/complete", workspaceId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"paymentId\":\"proof_1\",\"publicId\":\"public_id_path\"}"))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error.code").value("INTERNAL_SERVER_ERROR"))
                .andExpect(jsonPath("$.error.message").value("An unexpected error occurred: Cloudinary asset verification failed: connect timed out"));
    }
}
