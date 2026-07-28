package com.claimguard.claim.dto;

import com.claimguard.extraction.dto.ExtractionResponse;

import java.time.Instant;
import java.util.UUID;

public record DocumentResponse(
        UUID id,
        String filename,
        String contentType,
        long sizeBytes,
        Instant createdAt,
        ExtractionResponse extraction) {
}
