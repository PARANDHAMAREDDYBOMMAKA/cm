package com.claimguard.claim.dto;

import java.time.Instant;
import java.util.UUID;

public record ClaimStatusResponse(
        UUID id,
        String status,
        Instant updatedAt,
        int documentCount,
        boolean processing) {
}
