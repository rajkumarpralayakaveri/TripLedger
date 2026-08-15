package com.rkdevstudios.tripledger.contribution.application;

import com.rkdevstudios.tripledger.contribution.domain.CloudinaryAssetMetadata;
import java.util.Map;

public interface PaymentProofStorageService {
    Map<String, Object> generateUploadSignature(String publicId, long timestamp);
    CloudinaryAssetMetadata verifyUploadedAsset(String publicId);
    String generateSecureViewUrl(String publicId);
    void deleteAsset(String publicId);
}
