package com.claimguard.metrics.dto;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

public record MetricsResponse(
        long totalClaims,
        long decidedClaims,
        long autoApproved,
        long needsReview,
        long reviewerApproved,
        long escalated,
        double straightThroughRate,
        BigDecimal amountProcessed,
        BigDecimal leakageCaught,
        Double averageDecisionMinutes,
        long slaHours,
        long openBeyondSla,
        Map<String, Long> riskBands,
        List<SignalCount> topSignals,
        long auditEvents,
        boolean auditIntact) {
}
