package com.claimguard.fhir.dto;

import java.time.Instant;

public record NhcxSubmissionResponse(
        String correlationId,
        String claimReference,
        String participantCode,
        String status,
        boolean delivered,
        int bundleBytes,
        Instant preparedAt,
        String message) {
}
