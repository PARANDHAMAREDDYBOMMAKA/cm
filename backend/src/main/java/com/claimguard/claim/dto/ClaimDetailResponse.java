package com.claimguard.claim.dto;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record ClaimDetailResponse(
        UUID id,
        String reference,
        String claimantName,
        String note,
        String status,
        Instant createdAt,
        Instant updatedAt,
        List<DocumentResponse> documents) {
}
