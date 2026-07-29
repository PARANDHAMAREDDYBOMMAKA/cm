package com.claimguard.fraud;

import java.time.Instant;
import java.util.Map;

public record ForensicsReport(
        boolean hasMetadata,
        String software,
        String cameraMake,
        String cameraModel,
        Instant createdAt,
        Instant modifiedAt,
        String producer,
        Map<String, String> attributes) {

    public static ForensicsReport empty() {
        return new ForensicsReport(false, null, null, null, null, null, null, Map.of());
    }
}
