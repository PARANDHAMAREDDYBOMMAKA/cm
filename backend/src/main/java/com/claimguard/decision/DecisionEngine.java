package com.claimguard.decision;

import com.claimguard.audit.Actors;
import com.claimguard.audit.AuditAction;
import com.claimguard.audit.AuditService;
import com.claimguard.claim.Claim;
import com.claimguard.claim.ClaimRepository;
import com.claimguard.extraction.DocumentExtraction;
import com.claimguard.extraction.DocumentExtractionRepository;
import com.claimguard.extraction.ExtractionStatus;
import com.claimguard.fraud.FraudSignal;
import com.claimguard.fraud.RiskBand;
import com.claimguard.fraud.Severity;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Service
public class DecisionEngine {

    private static final int MAX_REASONS = 6;
    private static final Set<Severity> BLOCKING = Set.of(Severity.HIGH, Severity.CRITICAL);

    private final ClaimRepository claims;
    private final ClaimDecisionRepository decisions;
    private final DocumentExtractionRepository extractions;
    private final AuditService audit;
    private final ApplicationEventPublisher events;
    private final int autoApproveMaxScore;

    public DecisionEngine(ClaimRepository claims,
            ClaimDecisionRepository decisions,
            DocumentExtractionRepository extractions,
            AuditService audit,
            ApplicationEventPublisher events,
            @Value("${AUTO_APPROVE_MAX_SCORE:24}") int autoApproveMaxScore) {
        this.claims = claims;
        this.decisions = decisions;
        this.extractions = extractions;
        this.audit = audit;
        this.events = events;
        this.autoApproveMaxScore = autoApproveMaxScore;
    }

    @Transactional
    public void decide(Claim claim, List<FraudSignal> signals, RiskBand band, int score) {
        ClaimDecision existing = decisions.findById(claim.getId()).orElse(null);
        if (existing != null && !existing.isAutomatic()) {
            return;
        }

        List<String> reasons = reasons(claim, signals, band, score);
        DecisionOutcome outcome = reasons.isEmpty() ? DecisionOutcome.AUTO_APPROVED : DecisionOutcome.NEEDS_REVIEW;
        if (reasons.isEmpty()) {
            reasons = List.of("No fraud or consistency signals of concern.",
                    "Every document was read and the figures line up.");
        }

        ClaimDecision decision = existing != null ? existing : new ClaimDecision();
        decision.setClaimId(claim.getId());
        decision.setOutcome(outcome);
        decision.setAutomatic(true);
        decision.setDecidedBy(Actors.SYSTEM);
        decision.setNote(null);
        decision.setReasons(new ArrayList<>(reasons));
        decision.setDecidedAt(Instant.now());
        decisions.save(decision);

        if (claim.getStatus() != outcome.status()) {
            claim.setStatus(outcome.status());
            claims.save(claim);
        }

        Map<String, String> details = new LinkedHashMap<>();
        details.put("outcome", outcome.name());
        details.put("riskBand", band.name());
        details.put("riskScore", String.valueOf(score));
        details.put("reasons", String.join(" ", decision.getReasons()));
        audit.record(claim.getId(), claim.getReference(), AuditAction.DECISION_RECORDED,
                outcome == DecisionOutcome.AUTO_APPROVED
                        ? "Auto-approved: nothing of concern was found."
                        : "Routed to review: " + decision.getReasons().get(0),
                details);

        events.publishEvent(new ClaimDecidedEvent(claim.getId(), claim.getReference(), outcome, true, score,
                List.copyOf(decision.getReasons())));
    }

    private List<String> reasons(Claim claim, List<FraudSignal> signals, RiskBand band, int score) {
        List<String> reasons = new ArrayList<>();

        if (claim.getDocuments().isEmpty()) {
            reasons.add("No document has been submitted for this claim.");
            return reasons;
        }
        if (!hasReadDocument(claim)) {
            reasons.add("No document on this claim has been read yet.");
            return reasons;
        }

        if (band.needsReview() || score > autoApproveMaxScore) {
            reasons.add("The risk score is " + score + "/100 (" + band.name().toLowerCase() + ").");
        }

        signals.stream()
                .filter(signal -> BLOCKING.contains(signal.getSeverity()))
                .map(FraudSignal::getMessage)
                .forEach(reasons::add);

        return reasons.size() > MAX_REASONS ? new ArrayList<>(reasons.subList(0, MAX_REASONS)) : reasons;
    }

    private boolean hasReadDocument(Claim claim) {
        return claim.getDocuments().stream()
                .anyMatch(document -> extractions.findByDocumentId(document.getId())
                        .map(DocumentExtraction::getStatus)
                        .filter(status -> status == ExtractionStatus.COMPLETED)
                        .isPresent());
    }
}
