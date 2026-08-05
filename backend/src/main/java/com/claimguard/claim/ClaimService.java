package com.claimguard.claim;

import com.claimguard.access.AccessPolicy;
import com.claimguard.audit.AuditAction;
import com.claimguard.audit.AuditService;
import com.claimguard.claim.dto.ClaimDetailResponse;
import com.claimguard.claim.dto.ClaimStatusResponse;
import com.claimguard.claim.dto.ClaimSummaryResponse;
import com.claimguard.claim.dto.CreateClaimRequest;
import com.claimguard.claim.dto.DocumentResponse;
import com.claimguard.claim.dto.UpdateClaimRequest;
import com.claimguard.decision.DecisionLookup;
import com.claimguard.decision.DecisionOutcome;
import com.claimguard.decision.dto.DecisionResponse;
import com.claimguard.extraction.DocumentExtraction;
import com.claimguard.extraction.DocumentExtractionRepository;
import com.claimguard.extraction.DocumentUploadedEvent;
import com.claimguard.extraction.ExtractionMapper;
import com.claimguard.extraction.ExtractionStatus;
import com.claimguard.fraud.DocumentIntake;
import com.claimguard.fraud.RiskLookup;
import com.claimguard.fraud.dto.RiskResponse;
import com.claimguard.storage.StorageService;
import com.claimguard.web.dto.PageResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.core.io.InputStreamResource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class ClaimService {

    private static final Set<ClaimStatus> REVIEW_STATUSES = Set.of(ClaimStatus.FLAGGED, ClaimStatus.ESCALATED);
    private static final Set<DecisionOutcome> REVIEW_OUTCOMES = Set.of(
            DecisionOutcome.NEEDS_REVIEW, DecisionOutcome.FLAGGED, DecisionOutcome.ESCALATED);
    private static final int MAX_REFERENCE_ATTEMPTS = 5;

    private final ClaimRepository claims;
    private final ClaimDocumentRepository documents;
    private final DocumentExtractionRepository extractions;
    private final StorageService storage;
    private final DocumentIntake intake;
    private final RiskLookup riskLookup;
    private final DecisionLookup decisionLookup;
    private final AuditService audit;
    private final AccessPolicy access;
    private final UploadRateLimit uploadRateLimit;
    private final ApplicationEventPublisher events;
    private final long maxUploadBytes;

    public ClaimService(ClaimRepository claims,
            ClaimDocumentRepository documents,
            DocumentExtractionRepository extractions,
            StorageService storage,
            DocumentIntake intake,
            RiskLookup riskLookup,
            DecisionLookup decisionLookup,
            AuditService audit,
            AccessPolicy access,
            UploadRateLimit uploadRateLimit,
            ApplicationEventPublisher events,
            @Value("${UPLOAD_MAX_BYTES:26214400}") long maxUploadBytes) {
        this.claims = claims;
        this.documents = documents;
        this.extractions = extractions;
        this.storage = storage;
        this.intake = intake;
        this.riskLookup = riskLookup;
        this.decisionLookup = decisionLookup;
        this.audit = audit;
        this.access = access;
        this.uploadRateLimit = uploadRateLimit;
        this.events = events;
        this.maxUploadBytes = maxUploadBytes;
    }

    @Transactional
    public ClaimDetailResponse create(CreateClaimRequest request) {
        Claim claim = new Claim();
        claim.setReference(hasText(request.reference()) ? request.reference().trim() : generateReference());
        claim.setClaimantName(trimToNull(request.claimantName()));
        claim.setNote(trimToNull(request.note()));
        claim.setStatus(ClaimStatus.RECEIVED);
        claim.setOwnerSubject(access.ownerSubject());
        claim.setOwnerOrg(access.ownerOrg());
        if (claims.existsByReferenceIgnoreCase(claim.getReference())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "A claim with reference " + claim.getReference() + " already exists.");
        }
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
            String reference = request.reference().trim();
            if (!reference.equalsIgnoreCase(claim.getReference()) && claims.existsByReferenceIgnoreCase(reference)) {
                throw new ResponseStatusException(HttpStatus.CONFLICT,
                        "A claim with reference " + reference + " already exists.");
            }
            claim.setReference(reference);
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
        access.requireReviewer();
        claim.getDocuments().forEach(document -> storage.delete(document.getStorageKey()));
        audit.record(claim.getId(), claim.getReference(), AuditAction.CLAIM_DELETED,
                "Claim " + claim.getReference() + " was deleted.",
                Map.of("documents", String.valueOf(claim.getDocuments().size())));
        claims.delete(claim);
    }

    @Transactional(readOnly = true)
    public PageResponse<ClaimSummaryResponse> list(int page, int size) {
        Page<Claim> found = claims.findVisible(
                scopedSubject(), scopedOrg(), PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt")));
        return PageResponse.of(found, summariser(found.getContent()));
    }

    @Transactional(readOnly = true)
    public PageResponse<ClaimSummaryResponse> reviewQueue(int page, int size) {
        Page<Claim> found = claims.findReviewQueue(
                scopedSubject(), scopedOrg(), REVIEW_STATUSES, REVIEW_OUTCOMES,
                PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt")));
        List<ClaimSummaryResponse> ordered = found.getContent().stream()
                .map(summariser(found.getContent()))
                .sorted(Comparator.comparing(
                        (ClaimSummaryResponse summary) -> summary.riskScore() == null ? 0 : summary.riskScore())
                        .reversed())
                .toList();
        return new PageResponse<>(ordered, found.getNumber(), found.getSize(),
                found.getTotalElements(), found.getTotalPages());
    }

    @Transactional(readOnly = true)
    public ClaimDetailResponse get(UUID id) {
        return detail(require(id));
    }

    @Transactional(readOnly = true)
    public ClaimStatusResponse status(UUID id) {
        Claim claim = require(id);
        List<UUID> documentIds = claim.getDocuments().stream().map(ClaimDocument::getId).toList();
        List<DocumentExtraction> found = documentIds.isEmpty()
                ? List.of()
                : extractions.findByDocumentIdIn(documentIds);
        boolean processing = found.size() < documentIds.size()
                || found.stream().anyMatch(extraction -> extraction.getStatus() == ExtractionStatus.PENDING
                        || extraction.getStatus() == ExtractionStatus.RUNNING);
        return new ClaimStatusResponse(
                claim.getId(),
                claim.getStatus().name(),
                claim.getUpdatedAt(),
                documentIds.size(),
                processing);
    }

    @Transactional
    public DocumentResponse addDocument(UUID claimId, MultipartFile file, String deviceFingerprint) {
        uploadRateLimit.check();
        Claim claim = require(claimId);
        byte[] content = read(file);
        if (content.length == 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "The uploaded file is empty.");
        }
        if (content.length > maxUploadBytes) {
            throw new ResponseStatusException(HttpStatus.PAYLOAD_TOO_LARGE,
                    "The uploaded file exceeds the maximum allowed size.");
        }
        String contentType = UploadPolicy.sniff(content);
        if (!UploadPolicy.isAccepted(contentType)) {
            throw new ResponseStatusException(HttpStatus.UNSUPPORTED_MEDIA_TYPE,
                    "Only PDF and image documents are accepted (" + String.join(", ", UploadPolicy.acceptedTypes())
                            + ").");
        }

        String key = "claims/" + claim.getId() + "/" + UUID.randomUUID() + extension(contentType);
        StorageService.StoredObject stored = storage.store(
                key, new ByteArrayInputStream(content), content.length, contentType);

        ClaimDocument document = new ClaimDocument();
        document.setClaim(claim);
        document.setOriginalFilename(safeFilename(file.getOriginalFilename()));
        document.setContentType(contentType);
        document.setSizeBytes(stored.size());
        document.setStorageKey(stored.key());
        document.setDeviceFingerprint(trimToNull(deviceFingerprint));
        intake.applyContentHash(document, content);

        ClaimDocument saved = documents.save(document);
        audit.record(claim.getId(), claim.getReference(), AuditAction.DOCUMENT_UPLOADED,
                "Document " + saved.getOriginalFilename() + " was uploaded.",
                Map.of("filename", String.valueOf(saved.getOriginalFilename()),
                        "contentType", String.valueOf(saved.getContentType()),
                        "sizeBytes", String.valueOf(saved.getSizeBytes()),
                        "sha256", String.valueOf(saved.getContentSha256())));
        events.publishEvent(new DocumentUploadedEvent(saved.getId()));
        return document(saved, null);
    }

    @Transactional
    public void deleteDocument(UUID claimId, UUID documentId) {
        require(claimId);
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
        require(claimId);
        ClaimDocument document = documents.findById(documentId)
                .filter(candidate -> candidate.getClaim().getId().equals(claimId))
                .orElseThrow(() -> new ClaimNotFoundException(documentId));
        StorageService.RetrievedObject object = storage.retrieve(document.getStorageKey());
        String contentType = UploadPolicy.isAccepted(document.getContentType())
                ? document.getContentType()
                : "application/octet-stream";
        return new DocumentContent(
                new InputStreamResource(object.content()),
                contentType,
                document.getSizeBytes(),
                document.getOriginalFilename());
    }

    private Function<Claim, ClaimSummaryResponse> summariser(List<Claim> claimsOnPage) {
        List<UUID> ids = claimsOnPage.stream().map(Claim::getId).toList();
        Map<UUID, RiskResponse> risks = riskLookup.forClaims(ids);
        Map<UUID, DecisionResponse> decisions = decisionLookup.forClaims(ids);
        Map<UUID, Long> documentCounts = documentCounts(ids);
        return claim -> summary(
                claim,
                risks.get(claim.getId()),
                decisions.get(claim.getId()),
                documentCounts.getOrDefault(claim.getId(), 0L));
    }

    private Map<UUID, Long> documentCounts(List<UUID> claimIds) {
        Map<UUID, Long> counts = new HashMap<>();
        if (claimIds.isEmpty()) {
            return counts;
        }
        for (Object[] row : documents.countByClaimIds(claimIds)) {
            counts.put((UUID) row[0], ((Number) row[1]).longValue());
        }
        return counts;
    }

    private String scopedSubject() {
        return switch (access.scope()) {
            case USER -> nullToEmpty(access.ownerSubject());
            case NONE, ORG -> "";
        };
    }

    private String scopedOrg() {
        return switch (access.scope()) {
            case ORG -> nullToEmpty(access.ownerOrg());
            case NONE, USER -> "";
        };
    }

    private static String nullToEmpty(String value) {
        return value == null ? "" : value;
    }

    private static byte[] read(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "A file is required.");
        }
        try {
            return file.getBytes();
        } catch (IOException exception) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "The upload could not be read.");
        }
    }

    private Claim require(UUID id) {
        Claim claim = claims.findById(id).orElseThrow(() -> new ClaimNotFoundException(id));
        if (!access.canSee(claim.getOwnerSubject(), claim.getOwnerOrg())) {
            throw new ClaimNotFoundException(id);
        }
        return claim;
    }

    private static ClaimStatus parseStatus(String value) {
        try {
            return ClaimStatus.valueOf(value.trim().toUpperCase());
        } catch (IllegalArgumentException exception) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid status: " + value);
        }
    }

    private String generateReference() {
        for (int attempt = 0; attempt < MAX_REFERENCE_ATTEMPTS; attempt++) {
            String candidate = "CLM-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
            if (!claims.existsByReferenceIgnoreCase(candidate)) {
                return candidate;
            }
        }
        return "CLM-" + UUID.randomUUID().toString().replace("-", "").substring(0, 16).toUpperCase();
    }

    private static String extension(String contentType) {
        return switch (contentType) {
            case UploadPolicy.PDF -> ".pdf";
            case UploadPolicy.PNG -> ".png";
            case UploadPolicy.JPEG -> ".jpg";
            case UploadPolicy.WEBP -> ".webp";
            case UploadPolicy.TIFF -> ".tif";
            default -> "";
        };
    }

    private static String safeFilename(String filename) {
        if (filename == null || filename.isBlank()) {
            return "document";
        }
        String base = filename.replace('\\', '/');
        int slash = base.lastIndexOf('/');
        String stripped = slash >= 0 ? base.substring(slash + 1) : base;
        String cleaned = stripped.replaceAll("[\\p{Cntrl}\"\\\\]", "").trim();
        if (cleaned.isEmpty()) {
            return "document";
        }
        return cleaned.length() <= 255 ? cleaned : cleaned.substring(0, 255);
    }

    private static boolean hasText(String value) {
        return value != null && !value.isBlank();
    }

    private static String trimToNull(String value) {
        return hasText(value) ? value.trim() : null;
    }

    private static ClaimSummaryResponse summary(Claim claim,
            RiskResponse risk,
            DecisionResponse decision,
            long documentCount) {
        return new ClaimSummaryResponse(
                claim.getId(),
                claim.getReference(),
                claim.getClaimantName(),
                claim.getStatus().name(),
                (int) documentCount,
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
