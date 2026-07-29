package com.claimguard.fraud.dto;

import java.util.Map;
import java.util.UUID;

public record SignalResponse(
        UUID id,
        UUID documentId,
        String type,
        String severity,
        int weight,
        String message,
        Map<String, String> details) {
}
