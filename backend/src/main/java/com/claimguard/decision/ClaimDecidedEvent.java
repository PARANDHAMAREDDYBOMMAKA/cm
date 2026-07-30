package com.claimguard.decision;

import java.util.List;
import java.util.UUID;

public record ClaimDecidedEvent(
        UUID claimId,
        String reference,
        DecisionOutcome outcome,
        boolean automatic,
        int riskScore,
        List<String> reasons) {
}
