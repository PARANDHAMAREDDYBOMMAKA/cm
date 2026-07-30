package com.claimguard.metrics;

import com.claimguard.audit.AuditLookup;
import com.claimguard.audit.dto.AuditVerificationResponse;
import com.claimguard.claim.Claim;
import com.claimguard.claim.ClaimRepository;
import com.claimguard.decision.ClaimDecision;
import com.claimguard.decision.ClaimDecisionRepository;
import com.claimguard.decision.DecisionOutcome;
import com.claimguard.extraction.DocumentExtractionRepository;
import com.claimguard.fraud.ClaimRisk;
import com.claimguard.fraud.ClaimRiskRepository;
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
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

@Service
public class MetricsService {

    private static final int TOP_SIGNALS = 6;
    private static final Set<DecisionOutcome> LEAKING = EnumSet.of(
            DecisionOutcome.NEEDS_REVIEW, DecisionOutcome.FLAGGED, DecisionOutcome.ESCALATED);

    private final ClaimRepository claims;
    private final ClaimDecisionRepository decisions;
    private final ClaimRiskRepository risks;
    private final FraudSignalRepository signals;
    private final DocumentExtractionRepository extractions;
    private final AuditLookup audit;
    private final long slaHours;

    public MetricsService(ClaimRepository claims,
            ClaimDecisionRepository decisions,
            ClaimRiskRepository risks,
            FraudSignalRepository signals,
            DocumentExtractionRepository extractions,
            AuditLookup audit,
            @Value("${DECISION_SLA_HOURS:24}") long slaHours) {
        this.claims = claims;
        this.decisions = decisions;
        this.risks = risks;
        this.signals = signals;
        this.extractions = extractions;
        this.audit = audit;
        this.slaHours = slaHours;
    }

    @Transactional(readOnly = true)
    public MetricsResponse snapshot() {
        List<Claim> allClaims = claims.findAll();
        Map<UUID, ClaimDecision> byClaim = new HashMap<>();
        decisions.findAll().forEach(decision -> byClaim.put(decision.getClaimId(), decision));
        Map<UUID, BigDecimal> amounts = amountsByClaim();

        long autoApproved = count(byClaim, DecisionOutcome.AUTO_APPROVED);
        long needsReview = count(byClaim, DecisionOutcome.NEEDS_REVIEW);
        long reviewerApproved = count(byClaim, DecisionOutcome.APPROVED);
        long escalated = count(byClaim, DecisionOutcome.ESCALATED);
        long decided = byClaim.size();

        BigDecimal processed = BigDecimal.ZERO;
        BigDecimal leakage = BigDecimal.ZERO;
        long openBeyondSla = 0;
        long decisionMinutes = 0;
        Instant cutoff = Instant.now().minus(Duration.ofHours(slaHours));

        for (Claim claim : allClaims) {
            BigDecimal amount = amounts.getOrDefault(claim.getId(), BigDecimal.ZERO);
            processed = processed.add(amount);
            ClaimDecision decision = byClaim.get(claim.getId());
            if (decision == null) {
                if (claim.getCreatedAt().isBefore(cutoff)) {
                    openBeyondSla++;
                }
                continue;
            }
            if (LEAKING.contains(decision.getOutcome())) {
                leakage = leakage.add(amount);
            }
            decisionMinutes += Duration.between(claim.getCreatedAt(), decision.getDecidedAt()).toMinutes();
        }

        AuditVerificationResponse verification = audit.verify();

        return new MetricsResponse(
                allClaims.size(),
                decided,
                autoApproved,
                needsReview,
                reviewerApproved,
                escalated,
                rate(autoApproved, decided),
                processed,
                leakage,
                decided == 0 ? null : (double) decisionMinutes / decided,
                slaHours,
                openBeyondSla,
                bands(),
                topSignals(),
                verification.eventCount(),
                verification.intact());
    }

    private Map<UUID, BigDecimal> amountsByClaim() {
        Map<UUID, BigDecimal> amounts = new HashMap<>();
        for (Object[] row : extractions.maxTotalByClaim()) {
            amounts.put((UUID) row[0], (BigDecimal) row[1]);
        }
        return amounts;
    }

    private Map<String, Long> bands() {
        Map<String, Long> counts = new LinkedHashMap<>();
        for (ClaimRisk risk : risks.findAll()) {
            counts.merge(risk.getBand().name(), 1L, Long::sum);
        }
        return counts;
    }

    private List<SignalCount> topSignals() {
        return signals.countByType().stream()
                .limit(TOP_SIGNALS)
                .map(row -> new SignalCount(String.valueOf(row[0]), ((Number) row[1]).longValue()))
                .toList();
    }

    private static long count(Map<UUID, ClaimDecision> byClaim, DecisionOutcome outcome) {
        return byClaim.values().stream().filter(decision -> decision.getOutcome() == outcome).count();
    }

    private static double rate(long part, long total) {
        if (total == 0) {
            return 0;
        }
        return BigDecimal.valueOf(part * 100.0 / total).setScale(1, RoundingMode.HALF_UP).doubleValue();
    }
}
