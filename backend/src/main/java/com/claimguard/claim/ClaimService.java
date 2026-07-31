package com.claimguard.claim;

import com.claimguard.audit.AuditAction;
import com.claimguard.audit.AuditService;
import com.claimguard.claim.dto.ClaimDetailResponse;
import com.claimguard.claim.dto.ClaimSummaryResponse;
import com.claimguard.claim.dto.CreateClaimRequest;
import com.claimguard.claim.dto.DocumentResponse;
import com.claimguard.claim.dto.UpdateClaimRequest;
import com.claimguard.decision.DecisionLookup;
import com.claimguard.decision.dto.DecisionResponse;
import com.claimguard.extraction.DocumentExtraction;
import com.claimguard.extraction.DocumentExtractionRepository;
import com.claimguard.extraction.DocumentUploadedEvent;
import com.claimguard.extraction.ExtractionMapper;
import com.claimguard.fraud.DocumentIntake;
import com.claimguard.fraud.RiskLookup;
import com.claimguard.fraud.dto.RiskResponse;
import com.claimguard.storage.StorageService;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class ClaimService {

    private static final Set<String> REVIEW_OUTCOMES = Set.of("NEEDS_REVIEW", "FLAGGED", "ESCALATED");

    private final ClaimRepository claims;
    private final ClaimDocumentRepository documents;
    private final DocumentExtractionRepository extractions;
    private final StorageService storage;
    private final DocumentIntake intake;
    private final RiskLookup riskLookup;
    private final DecisionLookup decisionLookup;
    private final AuditService audit;
    private final ApplicationEventPublisher events;

    public ClaimService(ClaimRepository claims,
            ClaimDocumentRepository documents,
            DocumentExtractionRepository extractions,
            StorageService storage,
            DocumentIntake intake,
            RiskLookup riskLookup,
            DecisionLookup decisionLookup,
            AuditService audit,
            ApplicationEventPublisher events) {
        this.claims = claims;
        this.documents = documents;
        this.extractions = extractions;
        this.storage = storage;
        this.intake = intake;
        this.riskLookup = riskLookup;
        this.decisionLookup = decisionLookup;
        this.audit = audit;
        this.events = events;
    }

    @Transactional
    public ClaimDetailResponse create(CreateClaimRequest request) {
        Claim claim = new Claim();
        claim.setReference(hasText(request.reference()) ? request.reference().trim() : generateReference());
        claim.setClaimantName(trimToNull(request.claimantName()));
        claim.setNote(trimToNull(request.note()));
        claim.setStatus(ClaimStatus.RECEIVED);
        Claim saved = claims.save(claim);
        audit.record(saved.getId(), saved.getReference(), AuditAction.CLAIM_CREATED,
                "Claim " + saved.getReference() + " was created.",
                Map.of("claimant", String.valueOf(saved.getClaimantName())));
        events.publishEvent(new ClaimCreatedEvent(saved.getId(), saved.getReference()));
        return detail(saved);
    }

    @Transactional
    public ClaimDetailResponse update(UUID id, UpdateClaimRequest request) {
        Claim claim = require(id);
        if (hasText(request.reference())) {
            claim.setReference(request.reference().trim());
        }
        claim.setClaimantName(trimToNull(request.claimantName()));
        claim.setNote(trimToNull(request.note()));
        if (hasText(request.status())) {
            claim.setStatus(parseStatus(request.status()));
        }
        Claim saved = claims.save(claim);
        audit.record(saved.getId(), saved.getReference(), AuditAction.CLAIM_UPDATED,
                "Claim " + saved.getReference() + " was edited.",
                Map.of("status", saved.getStatus().name()));
        return detail(saved);
    }

    @Transactional
    public void delete(UUID id) {
        Claim claim = require(id);
        claim.getDocuments().forEach(document -> storage.delete(document.getStorageKey()));
        audit.record(claim.getId(), claim.getReference(), AuditAction.CLAIM_DELETED,
                "Claim " + claim.getReference() + " was deleted.",
                Map.of("documents", String.valueOf(claim.getDocuments().size())));
        claims.delete(claim);
    }

    @Transactional(readOnly = true)
    public List<ClaimSummaryResponse> list() {
        List<Claim> all = claims.findAllByOrderByCreatedAtDesc();
        List<UUID> ids = all.stream().map(Claim::getId).toList();
        Map<UUID, RiskResponse> risks = riskLookup.forClaims(ids);
        Map<UUID, DecisionResponse> decisions = decisionLookup.forClaims(ids);
        return all.stream()
                .map(claim -> summary(claim, risks.get(claim.getId()), decisions.get(claim.getId())))
                .toList();
    }

    @Transactional(readOnly = true)
    public List<ClaimSummaryResponse> reviewQueue() {
        List<Claim> all = claims.findAllByOrderByCreatedAtDesc();
        List<UUID> ids = all.stream().map(Claim::getId).toList();
        Map<UUID, RiskResponse> risks = riskLookup.forClaims(ids);
        Map<UUID, DecisionResponse> decisions = decisionLookup.forClaims(ids);
        return all.stream()
                .filter(claim -> needsReview(claim, decisions.get(claim.getId())))
                .map(claim -> summary(claim, risks.get(claim.getId()), decisions.get(claim.getId())))
                .sorted(Comparator.comparing(
                        (ClaimSummaryResponse summary) -> summary.riskScore() == null ? 0 : summary.riskScore())
                        .reversed())
                .toList();
    }

    @Transactional(readOnly = true)
    public ClaimDetailResponse get(UUID id) {
        return detail(require(id));
    }

    @Transactional
    public DocumentResponse addDocument(UUID claimId, MultipartFile file, String deviceFingerprint) {
        Claim claim = require(claimId);
        byte[] content = read(file);
        String key = "claims/" + claim.getId() + "/" + UUID.randomUUID() + extension(file.getOriginalFilename());
        StorageService.StoredObject stored = storage.store(
                key, new ByteArrayInputStream(content), content.length, file.getContentType());

        ClaimDocument document = new ClaimDocument();
        document.setClaim(claim);
        document.setOriginalFilename(file.getOriginalFilename());
        document.setContentType(file.getContentType());
        document.setSizeBytes(stored.size());
        document.setStorageKey(stored.key());
        document.setDeviceFingerprint(trimToNull(deviceFingerprint));
        intake.applyFingerprints(document, content);

        ClaimDocument saved = documents.save(document);
        intake.recordForensics(saved, content);
        audit.record(claim.getId(), claim.getReference(), AuditAction.DOCUMENT_UPLOADED,
                "Document " + saved.getOriginalFilename() + " was uploaded.",
                Map.of("filename", String.valueOf(saved.getOriginalFilename()),
                        "sizeBytes", String.valueOf(saved.getSizeBytes()),
                        "sha256", String.valueOf(saved.getContentSha256())));
        events.publishEvent(new DocumentUploadedEvent(saved.getId()));
        return document(saved, null);
    }

    @Transactional
    public void deleteDocument(UUID claimId, UUID documentId) {
        ClaimDocument document = documents.findById(documentId)
                .filter(candidate -> candidate.getClaim().getId().equals(claimId))
                .orElseThrow(() -> new ClaimNotFoundException(documentId));
        storage.delete(document.getStorageKey());
        audit.record(claimId, document.getClaim().getReference(), AuditAction.DOCUMENT_DELETED,
                "Document " + document.getOriginalFilename() + " was removed.",
                Map.of("filename", String.valueOf(document.getOriginalFilename())));
        documents.delete(document);
    }

    @Transactional(readOnly = true)
    public DocumentContent openDocument(UUID claimId, UUID documentId) {
        ClaimDocument document = documents.findById(documentId)
                .filter(candidate -> candidate.getClaim().getId().equals(claimId))
                .orElseThrow(() -> new ClaimNotFoundException(documentId));
        StorageService.RetrievedObject object = storage.retrieve(document.getStorageKey());
        String contentType = document.getContentType() != null ? document.getContentType() : object.contentType();
        return new DocumentContent(
                new InputStreamResource(object.content()),
                contentType,
                document.getSizeBytes(),
                document.getOriginalFilename());
    }

    private static byte[] read(MultipartFile file) {
        try {
            return file.getBytes();
        } catch (IOException exception) {
            throw new UncheckedIOException("Failed to read upload", exception);
        }
    }

    private Claim require(UUID id) {
        return claims.findById(id).orElseThrow(() -> new ClaimNotFoundException(id));
    }

    private static ClaimStatus parseStatus(String value) {
        try {
            return ClaimStatus.valueOf(value.trim().toUpperCase());
        } catch (IllegalArgumentException exception) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid status: " + value);
        }
    }

    private static String generateReference() {
        return "CLM-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
    }

    private static String extension(String filename) {
        if (filename == null) {
            return "";
        }
        int dot = filename.lastIndexOf('.');
        return dot >= 0 ? filename.substring(dot) : "";
    }

    private static boolean needsReview(Claim claim, DecisionResponse decision) {
        if (decision != null && REVIEW_OUTCOMES.contains(decision.outcome())) {
            return true;
        }
        return claim.getStatus() == ClaimStatus.FLAGGED || claim.getStatus() == ClaimStatus.ESCALATED;
    }

    private static boolean hasText(String value) {
        return value != null && !value.isBlank();
    }

    private static String trimToNull(String value) {
        return hasText(value) ? value.trim() : null;
    }

    private static ClaimSummaryResponse summary(Claim claim, RiskResponse risk, DecisionResponse decision) {
        return new ClaimSummaryResponse(
                claim.getId(),
                claim.getReference(),
                claim.getClaimantName(),
                claim.getStatus().name(),
                claim.getDocuments().size(),
                claim.getCreatedAt(),
                risk != null ? risk.score() : null,
                risk != null ? risk.band() : null,
                decision != null ? decision.outcome() : null);
    }

    private ClaimDetailResponse detail(Claim claim) {
        Map<UUID, DocumentExtraction> byDocument = extractionsFor(claim);
        List<DocumentResponse> docs = claim.getDocuments().stream()
                .map(document -> document(document, byDocument.get(document.getId())))
                .toList();
        return new ClaimDetailResponse(
                claim.getId(),
                claim.getReference(),
                claim.getClaimantName(),
                claim.getNote(),
                claim.getStatus().name(),
                claim.getCreatedAt(),
                claim.getUpdatedAt(),
                docs,
                riskLookup.forClaim(claim.getId()),
                decisionLookup.forClaim(claim.getId()));
    }

    private Map<UUID, DocumentExtraction> extractionsFor(Claim claim) {
        List<UUID> documentIds = claim.getDocuments().stream().map(ClaimDocument::getId).toList();
        if (documentIds.isEmpty()) {
            return Map.of();
        }
        return extractions.findByDocumentIdIn(documentIds).stream()
                .collect(Collectors.toMap(
                        extraction -> extraction.getDocument().getId(),
                        Function.identity()));
    }

    private static DocumentResponse document(ClaimDocument document, DocumentExtraction extraction) {
        return new DocumentResponse(
                document.getId(),
                document.getOriginalFilename(),
                document.getContentType(),
                document.getSizeBytes(),
                document.getCreatedAt(),
                ExtractionMapper.toResponse(extraction));
    }

    public record DocumentContent(InputStreamResource resource, String contentType, long size, String filename) {
    }
}
