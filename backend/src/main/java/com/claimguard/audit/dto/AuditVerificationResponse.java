package com.claimguard.audit.dto;

public record AuditVerificationResponse(
        boolean intact,
        long eventCount,
        Long brokenAtSeq,
        String headHash,
        String message) {
}
