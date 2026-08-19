package com.rkdevstudios.tripledger.contribution.persistence;

import com.cloudinary.Cloudinary;
import com.cloudinary.utils.ObjectUtils;
import com.rkdevstudios.tripledger.contribution.application.PaymentProofStorageService;
import com.rkdevstudios.tripledger.contribution.domain.CloudinaryAssetMetadata;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

@Service
public class CloudinaryPaymentProofStorageService implements PaymentProofStorageService {

    private final Cloudinary cloudinary;
    private final String cloudName;
    private final String apiKey;
    private final String apiSecret;

    public CloudinaryPaymentProofStorageService(
            @Value("${cloudinary.cloud-name}") String cloudName,
            @Value("${cloudinary.api-key}") String apiKey,
            @Value("${cloudinary.api-secret}") String apiSecret
    ) {
        this.cloudName = cloudName;
        this.apiKey = apiKey;
        this.apiSecret = apiSecret;
        
        System.out.println("Cloudinary Config Loaded Check: " +
                "cloudName=" + (cloudName != null && !cloudName.trim().isEmpty() ? "PRESENT" : "MISSING") + ", " +
                "apiKey=" + (apiKey != null && !apiKey.trim().isEmpty() ? "PRESENT" : "MISSING") + ", " +
                "apiSecret=" + (apiSecret != null && !apiSecret.trim().isEmpty() ? "PRESENT" : "MISSING"));

        Map<String, String> config = new HashMap<>();
        config.put("cloud_name", cloudName);
        config.put("api_key", apiKey);
        config.put("api_secret", apiSecret);
        this.cloudinary = new Cloudinary(config);
    }

    @Override
    public Map<String, Object> generateUploadSignature(String publicId, long timestamp) {
        Map<String, Object> params = new HashMap<>();
        params.put("public_id", publicId);
        params.put("timestamp", timestamp);
        params.put("type", "private");

        String signature = cloudinary.apiSignRequest(params, apiSecret);

        Map<String, Object> response = new HashMap<>();
        response.put("signature", signature);
        response.put("publicId", publicId);
        response.put("timestamp", timestamp);
        response.put("cloudName", cloudName);
        response.put("apiKey", apiKey);
        return response;
    }

    @Override
    public CloudinaryAssetMetadata verifyUploadedAsset(String publicId) {
        try {
            Map<?, ?> result = cloudinary.api().resource(publicId, ObjectUtils.emptyMap());
            
            String fetchedPublicId = (String) result.get("public_id");
            String resourceType = (String) result.get("resource_type");
            String format = (String) result.get("format");
            Number bytesNum = (Number) result.get("bytes");
            long bytes = bytesNum != null ? bytesNum.longValue() : 0L;

            return new CloudinaryAssetMetadata(fetchedPublicId, resourceType, format, bytes);
        } catch (Exception e) {
            throw new RuntimeException("Cloudinary asset verification failed: " + e.getMessage(), e);
        }
    }

    @Override
    public String generateSecureViewUrl(String publicId) {
        try {
            return cloudinary.url()
                    .secure(true)
                    .type("private")
                    .signed(true)
                    .generate(publicId);
        } catch (Exception e) {
            throw new RuntimeException("Failed to generate secure viewing URL", e);
        }
    }

    @Override
    public void deleteAsset(String publicId) {
        try {
            cloudinary.uploader().destroy(publicId, ObjectUtils.emptyMap());
        } catch (Exception e) {
            // Non-blocking log
        }
    }
}
