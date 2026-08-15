package com.rkdevstudios.tripledger.contribution.domain;

public record CloudinaryAssetMetadata(
    String publicId,
    String resourceType,
    String format,
    long bytes
) {}
