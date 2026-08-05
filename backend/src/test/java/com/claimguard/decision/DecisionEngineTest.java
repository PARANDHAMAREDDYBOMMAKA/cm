package com.claimguard.decision;

import com.claimguard.audit.AuditService;
import com.claimguard.claim.Claim;
import com.claimguard.claim.ClaimDocument;
import com.claimguard.claim.ClaimRepository;
import com.claimguard.claim.ClaimStatus;
import com.claimguard.extraction.DocumentExtraction;
import com.claimguard.extraction.DocumentExtractionRepository;
import com.claimguard.extraction.ExtractionStatus;
import com.claimguard.fraud.FraudSignal;
import com.claimguard.fraud.RiskBand;
import com.claimguard.fraud.Severity;
import com.claimguard.fraud.SignalBuilder;
import com.claimguard.fraud.SignalType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.context.ApplicationEventPublisher;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class DecisionEngineTest {

    private static final int AUTO_APPROVE_MAX_SCORE = 24;

    @Mock
    private ClaimRepository claims;
    @Mock
    private ClaimDecisionRepository decisions;
    @Mock
    private DocumentExtractionRepository extractions;
    @Mock
    private AuditService audit;
    @Mock
    private ApplicationEventPublisher events;

    private DecisionEngine engine;
    private Claim claim;
    private UUID documentId;

    @BeforeEach
    void setUp() {
        engine = new DecisionEngine(claims, decisions, extractions, audit, events, AUTO_APPROVE_MAX_SCORE);

        documentId = UUID.randomUUID();
        ClaimDocument document = new ClaimDocument();
        document.setId(documentId);

        claim = new Claim();
        claim.setId(UUID.randomUUID());
        claim.setReference("CLM-TEST");
        claim.setStatus(ClaimStatus.EXTRACTED);
        claim.getDocuments().add(document);

        when(decisions.findById(claim.getId())).thenReturn(Optional.empty());
        when(decisions.save(any(ClaimDecision.class))).thenAnswer(invocation -> invocation.getArgument(0));
        readDocument(ExtractionStatus.COMPLETED);
    }

    private void readDocument(ExtractionStatus status) {
        DocumentExtraction extraction = new DocumentExtraction();
        extraction.setStatus(status);
        when(extractions.findByDocumentId(documentId)).thenReturn(Optional.of(extraction));
    }

    private ClaimDecision decide(List<FraudSignal> signals, int score) {
        engine.decide(claim, signals, RiskBand.of(score), score);
        ArgumentCaptor<ClaimDecision> captor = ArgumentCaptor.forClass(ClaimDecision.class);
        verify(decisions).save(captor.capture());
        return captor.getValue();
    }

    private static FraudSignal signal(Severity severity, String message) {
        return SignalBuilder.of(UUID.randomUUID(), UUID.randomUUID(), SignalType.EXACT_DUPLICATE,
                severity, message, Map.of());
    }

    @Test
    void autoApprovesACleanReadClaim() {
        ClaimDecision decision = decide(List.of(), 0);

        assertThat(decision.getOutcome()).isEqualTo(DecisionOutcome.AUTO_APPROVED);
        assertThat(decision.isAutomatic()).isTrue();
        assertThat(claim.getStatus()).isEqualTo(ClaimStatus.APPROVED);
    }

    @Test
    void aSingleCriticalSignalBlocksAutoApproval() {
        ClaimDecision decision = decide(List.of(signal(Severity.CRITICAL, "This exact file was already claimed.")), 0);

        assertThat(decision.getOutcome()).isEqualTo(DecisionOutcome.NEEDS_REVIEW);
        assertThat(decision.getReasons()).contains("This exact file was already claimed.");
    }

    @Test
    void aSingleHighSignalBlocksAutoApprovalEvenWhenTheScoreIsUnderTheThreshold() {
        ClaimDecision decision = decide(List.of(signal(Severity.HIGH, "Edited in Photoshop.")), 0);

        assertThat(decision.getOutcome()).isEqualTo(DecisionOutcome.NEEDS_REVIEW);
        assertThat(decision.getReasons()).contains("Edited in Photoshop.");
    }

    @Test
    void lowAndMediumSignalsAloneDoNotBlockAutoApproval() {
        ClaimDecision decision = decide(
                List.of(signal(Severity.LOW, "No metadata."), signal(Severity.MEDIUM, "Shared device.")), 0);

        assertThat(decision.getOutcome()).isEqualTo(DecisionOutcome.AUTO_APPROVED);
    }

    @Test
    void aScoreAboveTheThresholdRoutesToReview() {
        ClaimDecision decision = decide(List.of(), AUTO_APPROVE_MAX_SCORE + 1);

        assertThat(decision.getOutcome()).isEqualTo(DecisionOutcome.NEEDS_REVIEW);
        assertThat(decision.getReasons().get(0)).contains("risk score is 25/100");
    }

    @Test
    void aScoreExactlyAtTheThresholdStillAutoApproves() {
        assertThat(decide(List.of(), AUTO_APPROVE_MAX_SCORE).getOutcome())
                .isEqualTo(DecisionOutcome.AUTO_APPROVED);
    }

    @Test
    void aClaimWithNoDocumentsIsNeverAutoApproved() {
        claim.getDocuments().clear();

        ClaimDecision decision = decide(List.of(), 0);

        assertThat(decision.getOutcome()).isEqualTo(DecisionOutcome.NEEDS_REVIEW);
        assertThat(decision.getReasons()).containsExactly("No document has been submitted for this claim.");
    }

    @Test
    void aClaimWhoseDocumentsWereNotReadIsNeverAutoApproved() {
        readDocument(ExtractionStatus.FAILED);

        ClaimDecision decision = decide(List.of(), 0);

        assertThat(decision.getOutcome()).isEqualTo(DecisionOutcome.NEEDS_REVIEW);
        assertThat(decision.getReasons()).containsExactly("No document on this claim has been read yet.");
    }

    @Test
    void reasonsAreCappedSoTheDecisionStaysReadable() {
        List<FraudSignal> signals = List.of(
                signal(Severity.CRITICAL, "one"), signal(Severity.CRITICAL, "two"),
                signal(Severity.CRITICAL, "three"), signal(Severity.CRITICAL, "four"),
                signal(Severity.CRITICAL, "five"), signal(Severity.CRITICAL, "six"),
                signal(Severity.CRITICAL, "seven"), signal(Severity.CRITICAL, "eight"));

        assertThat(decide(signals, 90).getReasons()).hasSize(6);
    }

    @Test
    void doesNotOverturnADecisionAReviewerAlreadyMade() {
        ClaimDecision reviewed = new ClaimDecision();
        reviewed.setClaimId(claim.getId());
        reviewed.setOutcome(DecisionOutcome.APPROVED);
        reviewed.setAutomatic(false);
        when(decisions.findById(claim.getId())).thenReturn(Optional.of(reviewed));

        engine.decide(claim, List.of(signal(Severity.CRITICAL, "Duplicate.")), RiskBand.CRITICAL, 90);

        verify(decisions, never()).save(any(ClaimDecision.class));
        verify(events, never()).publishEvent(any(ClaimDecidedEvent.class));
    }
}
