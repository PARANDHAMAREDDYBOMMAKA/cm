package com.claimguard.claim.dto;

import java.time.Instant;
import java.util.UUID;

public record ClaimSummaryResponse(
        UUID id,
        String reference,
        String claimantName,
        String status,
        int documentCount,
        Instant createdAt,
        Integer riskScore,
        String riskBand) {
}
