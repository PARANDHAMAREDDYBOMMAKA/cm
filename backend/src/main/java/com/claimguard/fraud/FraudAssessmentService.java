package com.claimguard.fraud;

import com.claimguard.audit.AuditAction;
import com.claimguard.audit.AuditService;
import com.claimguard.claim.Claim;
import com.claimguard.claim.ClaimDocument;
import com.claimguard.claim.ClaimDocumentRepository;
import com.claimguard.claim.ClaimRepository;
import com.claimguard.decision.DecisionEngine;
import com.claimguard.extraction.DocumentExtraction;
import com.claimguard.extraction.DocumentExtractionRepository;
import com.claimguard.extraction.ExtractionLineItem;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@Service
public class FraudAssessmentService {

    private static final Logger log = LoggerFactory.getLogger(FraudAssessmentService.class);

    private static final int NEAR_DUPLICATE_DISTANCE = 6;
    private static final double SEMANTIC_SIMILARITY = 0.94;
    private static final int MAX_SCORE = 100;

    private final ClaimRepository claims;
    private final ClaimDocumentRepository documents;
    private final DocumentExtractionRepository extractions;
    private final DocumentForensicsRepository forensics;
    private final FraudSignalRepository signals;
    private final ClaimRiskRepository risks;
    private final DocumentEmbeddingStore embeddings;
    private final EmbeddingProvider embeddingProvider;
    private final DocumentIntake intake;
    private final ConsistencyInspector consistency;
    private final DecisionEngine decisions;
    private final AuditService audit;

    public FraudAssessmentService(ClaimRepository claims,
            ClaimDocumentRepository documents,
            DocumentExtractionRepository extractions,
            DocumentForensicsRepository forensics,
            FraudSignalRepository signals,
            ClaimRiskRepository risks,
            DocumentEmbeddingStore embeddings,
            EmbeddingProvider embeddingProvider,
            DocumentIntake intake,
            ConsistencyInspector consistency,
            DecisionEngine decisions,
            AuditService audit) {
        this.claims = claims;
        this.documents = documents;
        this.extractions = extractions;
        this.forensics = forensics;
        this.signals = signals;
        this.risks = risks;
        this.embeddings = embeddings;
        this.embeddingProvider = embeddingProvider;
        this.intake = intake;
        this.consistency = consistency;
        this.decisions = decisions;
        this.audit = audit;
    }

    @Transactional
    public void assess(UUID claimId) {
        Claim claim = claims.findById(claimId).orElse(null);
        if (claim == null) {
            return;
        }

        signals.deleteByClaimId(claimId);
        List<FraudSignal> found = new ArrayList<>();

        for (ClaimDocument document : claim.getDocuments()) {
            intake.backfill(document);
            found.addAll(duplicateSignals(claim, document));
            found.addAll(forensicSignals(claim, document));
            found.addAll(deviceSignals(claim, document));
            found.addAll(semanticSignals(claim, document));
            found.addAll(consistencySignals(claim, document));
        }

        signals.saveAll(found);
        ClaimRisk risk = applyRisk(claim, found);
        audit.record(claim.getId(), claim.getReference(), AuditAction.RISK_ASSESSED,
                "Risk assessed as " + risk.getBand().name().toLowerCase() + " (" + risk.getScore() + "/100).",
                Map.of("band", risk.getBand().name(),
                        "score", String.valueOf(risk.getScore()),
                        "signals", String.valueOf(found.size())));
        decisions.decide(claim, found, risk.getBand(), risk.getScore());
    }

    private List<FraudSignal> consistencySignals(Claim claim, ClaimDocument document) {
        return extractions.findByDocumentId(document.getId())
                .map(extraction -> consistency.inspect(claim, document, extraction))
                .orElseGet(List::of);
    }

    private List<FraudSignal> duplicateSignals(Claim claim, ClaimDocument document) {
        List<FraudSignal> found = new ArrayList<>();

        if (document.getContentSha256() != null) {
            for (ClaimDocument other : documents.findDuplicatesByHash(document.getContentSha256(), claim.getId())) {
                found.add(SignalBuilder.of(claim.getId(), document.getId(), SignalType.EXACT_DUPLICATE,
                        Severity.CRITICAL,
                        "This exact file was already submitted on claim " + other.getClaim().getReference() + ".",
                        Map.of("otherClaim", other.getClaim().getReference(),
                                "otherDocument", other.getOriginalFilename())));
            }
        }

        if (document.getPerceptualHash() != null && found.isEmpty()) {
            for (ClaimDocument other : documents.findHashedOutsideClaim(claim.getId())) {
                int distance = PerceptualHash.distance(document.getPerceptualHash(), other.getPerceptualHash());
                if (distance <= NEAR_DUPLICATE_DISTANCE) {
                    found.add(SignalBuilder.of(claim.getId(), document.getId(), SignalType.NEAR_DUPLICATE_IMAGE,
                            Severity.HIGH,
                            "This document looks near-identical to one on claim "
                                    + other.getClaim().getReference() + ".",
                            Map.of("otherClaim", other.getClaim().getReference(),
                                    "hammingDistance", String.valueOf(distance))));
                    break;
                }
            }
        }

        extractions.findByDocumentId(document.getId())
                .map(DocumentExtraction::getInvoiceNumber)
                .filter(invoice -> invoice != null && !invoice.isBlank())
                .ifPresent(invoice -> found.addAll(invoiceSignals(claim, document, invoice)));

        return found;
    }

    private List<FraudSignal> invoiceSignals(Claim claim, ClaimDocument document, String invoice) {
        List<FraudSignal> found = new ArrayList<>();
        for (DocumentExtraction other : extractions.findByInvoiceNumberIgnoreCase(invoice)) {
            UUID otherClaimId = other.getDocument().getClaim().getId();
            if (otherClaimId.equals(claim.getId())) {
                continue;
            }
            found.add(SignalBuilder.of(claim.getId(), document.getId(), SignalType.REUSED_INVOICE_NUMBER,
                    Severity.HIGH,
                    "Invoice number " + invoice + " was already claimed on "
                            + other.getDocument().getClaim().getReference() + ".",
                    Map.of("invoiceNumber", invoice,
                            "otherClaim", other.getDocument().getClaim().getReference())));
            break;
        }
        return found;
    }

    private List<FraudSignal> forensicSignals(Claim claim, ClaimDocument document) {
        Optional<DocumentForensics> report = forensics.findByDocumentId(document.getId());
        if (report.isEmpty()) {
            return List.of();
        }
        DocumentForensics data = report.get();
        List<FraudSignal> found = new ArrayList<>();

        String software = data.getSoftware() != null ? data.getSoftware() : data.getProducer();
        if (software != null && EditingSoftware.isEditor(software)) {
            found.add(SignalBuilder.of(claim.getId(), document.getId(), SignalType.EDITING_SOFTWARE,
                    Severity.HIGH,
                    "The document was last written by image editing software (" + software + ").",
                    Map.of("software", software)));
        }

        if (!data.isHasMetadata()) {
            found.add(SignalBuilder.of(claim.getId(), document.getId(), SignalType.METADATA_STRIPPED,
                    Severity.LOW,
                    "The document carries no metadata, which is common after re-saving or scrubbing.",
                    Map.of()));
        }

        if (data.getSourceCreatedAt() != null && data.getSourceModifiedAt() != null
                && data.getSourceModifiedAt().isAfter(data.getSourceCreatedAt().plusSeconds(60))) {
            found.add(SignalBuilder.of(claim.getId(), document.getId(), SignalType.MODIFIED_AFTER_CREATION,
                    Severity.MEDIUM,
                    "The document was modified after it was created.",
                    Map.of("created", data.getSourceCreatedAt().toString(),
                            "modified", data.getSourceModifiedAt().toString())));
        }

        if (data.getAiGeneratedScore() != null && data.getAiGeneratedScore().doubleValue() >= 0.7) {
            found.add(SignalBuilder.of(claim.getId(), document.getId(), SignalType.AI_GENERATED,
                    Severity.CRITICAL,
                    "The document is likely AI generated.",
                    Map.of("probability", data.getAiGeneratedScore().toPlainString(),
                            "detector", String.valueOf(data.getAiDetector()))));
        }

        return found;
    }

    private List<FraudSignal> deviceSignals(Claim claim, ClaimDocument document) {
        String fingerprint = document.getDeviceFingerprint();
        if (fingerprint == null || fingerprint.isBlank()) {
            return List.of();
        }
        List<ClaimDocument> others = documents.findByFingerprintOutsideClaim(fingerprint, claim.getId());
        Map<String, String> claimants = new LinkedHashMap<>();
        for (ClaimDocument other : others) {
            String claimant = other.getClaim().getClaimantName();
            if (claimant != null && !claimant.equalsIgnoreCase(claim.getClaimantName())) {
                claimants.put(other.getClaim().getReference(), claimant);
            }
        }
        if (claimants.isEmpty()) {
            return List.of();
        }
        return List.of(SignalBuilder.of(claim.getId(), document.getId(), SignalType.SHARED_DEVICE,
                Severity.MEDIUM,
                "The same device submitted claims for " + claimants.size() + " other claimant(s).",
                claimants));
    }

    private List<FraudSignal> semanticSignals(Claim claim, ClaimDocument document) {
        if (!embeddingProvider.isAvailable()) {
            return List.of();
        }
        Optional<DocumentExtraction> extraction = extractions.findByDocumentId(document.getId());
        if (extraction.isEmpty()) {
            return List.of();
        }
        String summary = summarize(extraction.get());
        if (summary.isBlank()) {
            return List.of();
        }
        try {
            float[] vector = embeddingProvider.embed(summary);
            List<DocumentEmbeddingStore.Match> matches =
                    embeddings.findSimilar(claim.getId(), vector, SEMANTIC_SIMILARITY, 5);
            embeddings.save(document.getId(), claim.getId(), embeddingProvider.modelName(), vector);

            List<FraudSignal> found = new ArrayList<>();
            for (DocumentEmbeddingStore.Match match : matches) {
                claims.findById(match.claimId()).ifPresent(other -> found.add(
                        SignalBuilder.of(claim.getId(), document.getId(), SignalType.SEMANTIC_DUPLICATE,
                                Severity.MEDIUM,
                                "The contents match claim " + other.getReference() + " very closely.",
                                Map.of("otherClaim", other.getReference(),
                                        "similarity", String.format("%.3f", match.similarity())))));
                break;
            }
            return found;
        } catch (RuntimeException exception) {
            log.warn("Semantic duplicate check failed for document {}: {}", document.getId(), exception.getMessage());
            return List.of();
        }
    }

    private static String summarize(DocumentExtraction extraction) {
        StringBuilder text = new StringBuilder();
        append(text, extraction.getProviderName());
        append(text, extraction.getPatientName());
        append(text, extraction.getDiagnosis());
        append(text, extraction.getInvoiceNumber());
        append(text, extraction.getTotalAmount() != null ? extraction.getTotalAmount().toPlainString() : null);
        for (ExtractionLineItem item : extraction.getLineItems()) {
            append(text, item.getDescription());
        }
        return text.toString().trim();
    }

    private static void append(StringBuilder text, String value) {
        if (value != null && !value.isBlank()) {
            text.append(value).append(' ');
        }
    }

    private ClaimRisk applyRisk(Claim claim, List<FraudSignal> found) {
        int score = Math.min(MAX_SCORE, found.stream().mapToInt(FraudSignal::getWeight).sum());

        ClaimRisk risk = risks.findById(claim.getId()).orElseGet(() -> {
            ClaimRisk created = new ClaimRisk();
            created.setClaimId(claim.getId());
            return created;
        });
        risk.setScore(score);
        risk.setBand(RiskBand.of(score));
        risk.setSignalCount(found.size());
        risk.setAssessedAt(Instant.now());
        return risks.save(risk);
    }
}
