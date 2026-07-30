package com.claimguard.audit.dto;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

public record AuditEventResponse(
        long seq,
        UUID id,
        UUID claimId,
        String claimReference,
        String actor,
        String action,
        String summary,
        Map<String, String> details,
        String previousHash,
        String hash,
        Instant createdAt) {
}
