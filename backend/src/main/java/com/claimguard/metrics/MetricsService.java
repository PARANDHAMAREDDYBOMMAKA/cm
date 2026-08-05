package com.claimguard.metrics;

import com.claimguard.audit.AuditLookup;
import com.claimguard.audit.dto.AuditVerificationResponse;
import com.claimguard.decision.DecisionOutcome;
import com.claimguard.fraud.FraudSignalRepository;
import com.claimguard.metrics.dto.MetricsResponse;
import com.claimguard.metrics.dto.SignalCount;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.time.Instant;
import java.util.EnumSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Service
public class MetricsService {

    private static final int TOP_SIGNALS = 6;
    private static final Set<DecisionOutcome> LEAKING = EnumSet.of(
            DecisionOutcome.NEEDS_REVIEW, DecisionOutcome.FLAGGED, DecisionOutcome.ESCALATED);

    private final MetricsRepository metrics;
    private final FraudSignalRepository signals;
    private final AuditLookup audit;
    private final long slaHours;

    public MetricsService(MetricsRepository metrics,
            FraudSignalRepository signals,
            AuditLookup audit,
            @Value("${DECISION_SLA_HOURS:24}") long slaHours) {
        this.metrics = metrics;
        this.signals = signals;
        this.audit = audit;
        this.slaHours = slaHours;
    }

    @Transactional(readOnly = true)
    public MetricsResponse snapshot() {
        Map<String, Long> outcomes = countByOutcome();
        long decided = metrics.countDecisions();
        long autoApproved = outcomes.getOrDefault(DecisionOutcome.AUTO_APPROVED.name(), 0L);
        long needsReview = outcomes.getOrDefault(DecisionOutcome.NEEDS_REVIEW.name(), 0L);
        long reviewerApproved = outcomes.getOrDefault(DecisionOutcome.APPROVED.name(), 0L);
        long flagged = outcomes.getOrDefault(DecisionOutcome.FLAGGED.name(), 0L);
        long escalated = outcomes.getOrDefault(DecisionOutcome.ESCALATED.name(), 0L);

        Instant cutoff = Instant.now().minus(Duration.ofHours(slaHours));
        BigDecimal processed = orZero(metrics.totalAmountProcessed());
        BigDecimal leakage = orZero(metrics.leakageCaught(
                LEAKING.stream().map(DecisionOutcome::name).toList()));
        Double averageMinutes = metrics.averageDecisionMinutes();

        AuditVerificationResponse verification = audit.summary();

        return new MetricsResponse(
                metrics.countClaims(),
                decided,
                autoApproved,
                needsReview,
                reviewerApproved,
                flagged,
                escalated,
                needsReview + flagged + escalated,
                rate(autoApproved, decided),
                processed,
                leakage,
                averageMinutes,
                slaHours,
                metrics.countOpenBeyondSla(cutoff),
                bands(),
                topSignals(),
                verification.eventCount(),
                verification.intact());
    }

    private Map<String, Long> countByOutcome() {
        Map<String, Long> counts = new LinkedHashMap<>();
        for (Object[] row : metrics.countByOutcome()) {
            counts.put(String.valueOf(row[0]), ((Number) row[1]).longValue());
        }
        return counts;
    }

    private Map<String, Long> bands() {
        Map<String, Long> counts = new LinkedHashMap<>();
        for (Object[] row : metrics.countByRiskBand()) {
            counts.put(String.valueOf(row[0]), ((Number) row[1]).longValue());
        }
        return counts;
    }

    private List<SignalCount> topSignals() {
        return signals.countByType().stream()
                .limit(TOP_SIGNALS)
                .map(row -> new SignalCount(String.valueOf(row[0]), ((Number) row[1]).longValue()))
                .toList();
    }

    private static BigDecimal orZero(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
    }

    private static double rate(long part, long total) {
        if (total == 0) {
            return 0;
        }
        return BigDecimal.valueOf(part * 100.0 / total).setScale(1, RoundingMode.HALF_UP).doubleValue();
    }
}
