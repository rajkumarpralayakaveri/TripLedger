package com.rkdevstudios.tripledger.contribution;

import com.rkdevstudios.tripledger.contribution.persistence.CloudinaryPaymentProofStorageService;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import java.util.HashMap;
import java.util.Map;
import com.cloudinary.Cloudinary;
import com.cloudinary.Api;

public class CloudinaryIntegrationTest {

    @Test
    public void testCloudinaryConnectionAndPermissions() {
        String cloudName = System.getenv("CLOUDINARY_CLOUD_NAME");
        String apiKey = System.getenv("CLOUDINARY_API_KEY");
        String apiSecret = System.getenv("CLOUDINARY_API_SECRET");

        if (cloudName == null || apiKey == null || apiSecret == null) {
            System.out.println("Skipping integration test: environment variables not set.");
            return;
        }

        CloudinaryPaymentProofStorageService storageService = new CloudinaryPaymentProofStorageService(
            cloudName, apiKey, apiSecret
        );

        // 1. Verify we can generate upload signatures locally
        var signatureData = storageService.generateUploadSignature("test_public_id", System.currentTimeMillis() / 1000L);
        assertNotNull(signatureData);
        assertEquals("test_public_id", signatureData.get("publicId"));
        assertNotNull(signatureData.get("signature"));

        // 2. Verify secure delivery URL generation
        String viewUrl = storageService.generateSecureViewUrl("test_public_id");
        assertNotNull(viewUrl);
        assertTrue(viewUrl.contains("cloudinary.com"));

        // 3. Try to call the resource API to verify credentials validity
        try {
            storageService.verifyUploadedAsset("non_existing_test_public_id");
        } catch (Exception e) {
            String errorMsg = e.getMessage().toLowerCase();
            System.out.println("API call returned exception: " + e.getMessage());
            assertFalse(errorMsg.contains("unauthorized") || errorMsg.contains("invalid api key") || errorMsg.contains("cannot authenticate"),
                    "Cloudinary credentials authentication failed: " + e.getMessage());
        }
    }

    @Test
    public void testVerifyUploadedAssetRequestsPrivateType() throws Exception {
        Cloudinary mockCloudinary = mock(Cloudinary.class);
        Api mockApi = mock(Api.class);
        when(mockCloudinary.api()).thenReturn(mockApi);

        com.cloudinary.api.ApiResponse mockResponse = mock(com.cloudinary.api.ApiResponse.class);
        when(mockResponse.get("public_id")).thenReturn("test_public_id");
        when(mockResponse.get("resource_type")).thenReturn("image");
        when(mockResponse.get("format")).thenReturn("jpg");
        when(mockResponse.get("bytes")).thenReturn(1000L);

        when(mockApi.resource(eq("test_public_id"), anyMap())).thenReturn(mockResponse);

        CloudinaryPaymentProofStorageService storageService = new CloudinaryPaymentProofStorageService(mockCloudinary);
        var metadata = storageService.verifyUploadedAsset("test_public_id");

        assertNotNull(metadata);
        assertEquals("test_public_id", metadata.publicId());
        assertEquals("image", metadata.resourceType());
        assertEquals("jpg", metadata.format());
        assertEquals(1000L, metadata.bytes());

        // Verify that Api.resource was called with options containing type=private
        verify(mockApi).resource(eq("test_public_id"), argThat(options -> 
            options != null && "private".equals(options.get("type"))
        ));
    }
}
