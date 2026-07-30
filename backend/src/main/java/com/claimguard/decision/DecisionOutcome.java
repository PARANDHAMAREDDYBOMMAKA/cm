package com.claimguard.decision;

import com.claimguard.claim.ClaimStatus;

public enum DecisionOutcome {
    AUTO_APPROVED(ClaimStatus.APPROVED),
    NEEDS_REVIEW(ClaimStatus.FLAGGED),
    APPROVED(ClaimStatus.APPROVED),
    FLAGGED(ClaimStatus.FLAGGED),
    ESCALATED(ClaimStatus.ESCALATED);

    private final ClaimStatus status;

    DecisionOutcome(ClaimStatus status) {
        this.status = status;
    }

    public ClaimStatus status() {
        return status;
    }
}
