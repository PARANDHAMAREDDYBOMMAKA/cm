package com.claimguard.claim;

import java.util.Set;

public enum ClaimStatus {
    RECEIVED,
    PROCESSING,
    EXTRACTED,
    FAILED,
    APPROVED,
    FLAGGED,
    ESCALATED;

    private static final Set<ClaimStatus> PIPELINE_OWNED = Set.of(RECEIVED, PROCESSING, EXTRACTED, FAILED);

    public boolean isPipelineOwned() {
        return PIPELINE_OWNED.contains(this);
    }
}
