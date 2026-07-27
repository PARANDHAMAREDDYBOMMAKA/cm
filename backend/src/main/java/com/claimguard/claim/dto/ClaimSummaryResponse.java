package com.claimguard.claim.dto;

import java.time.Instant;
import java.util.UUID;

public record ClaimSummaryResponse(
        UUID id,
        String reference,
        String status,
        int documentCount,
        Instant createdAt) {
}
