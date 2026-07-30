package com.claimguard.decision.dto;

import java.time.Instant;
import java.util.List;

public record DecisionResponse(
        String outcome,
        boolean automatic,
        String decidedBy,
        String note,
        List<String> reasons,
        Instant decidedAt) {

    public static DecisionResponse pending() {
        return new DecisionResponse("PENDING", true, null, null, List.of(), null);
    }
}
