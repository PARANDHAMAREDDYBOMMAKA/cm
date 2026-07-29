package com.claimguard.fraud.dto;

import java.time.Instant;
import java.util.List;

public record RiskResponse(
        int score,
        String band,
        int signalCount,
        Instant assessedAt,
        List<SignalResponse> signals) {

    public static RiskResponse empty() {
        return new RiskResponse(0, "UNASSESSED", 0, null, List.of());
    }
}
