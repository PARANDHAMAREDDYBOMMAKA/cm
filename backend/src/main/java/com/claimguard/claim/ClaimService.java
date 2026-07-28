package com.claimguard.claim;

import com.claimguard.claim.dto.ClaimDetailResponse;
import com.claimguard.claim.dto.ClaimSummaryResponse;
import com.claimguard.claim.dto.CreateClaimRequest;
import com.claimguard.claim.dto.DocumentResponse;
import com.claimguard.claim.dto.UpdateClaimRequest;
import com.claimguard.extraction.DocumentExtraction;
import com.claimguard.extraction.DocumentExtractionRepository;
import com.claimguard.extraction.DocumentUploadedEvent;
import com.claimguard.extraction.ExtractionMapper;
import com.claimguard.storage.StorageService;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class ClaimService {

    private final ClaimRepository claims;
    private final ClaimDocumentRepository documents;
    private final DocumentExtractionRepository extractions;
    private final StorageService storage;
    private final ApplicationEventPublisher events;

    public ClaimService(ClaimRepository claims,
            ClaimDocumentRepository documents,
            DocumentExtractionRepository extractions,
            StorageService storage,
            ApplicationEventPublisher events) {
        this.claims = claims;
        this.documents = documents;
        this.extractions = extractions;
        this.storage = storage;
        this.events = events;
    }

    @Transactional
    public ClaimDetailResponse create(CreateClaimRequest request) {
        Claim claim = new Claim();
        claim.setReference(hasText(request.reference()) ? request.reference().trim() : generateReference());
        claim.setClaimantName(trimToNull(request.claimantName()));
        claim.setNote(trimToNull(request.note()));
        claim.setStatus(ClaimStatus.RECEIVED);
        return detail(claims.save(claim));
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
        return detail(claims.save(claim));
    }

    @Transactional
    public void delete(UUID id) {
        Claim claim = require(id);
        claim.getDocuments().forEach(document -> storage.delete(document.getStorageKey()));
        claims.delete(claim);
    }

    @Transactional(readOnly = true)
    public List<ClaimSummaryResponse> list() {
        return claims.findAllByOrderByCreatedAtDesc().stream().map(ClaimService::summary).toList();
    }

    @Transactional(readOnly = true)
    public ClaimDetailResponse get(UUID id) {
        return detail(require(id));
    }

    @Transactional
    public DocumentResponse addDocument(UUID claimId, MultipartFile file) {
        Claim claim = require(claimId);
        String key = "claims/" + claim.getId() + "/" + UUID.randomUUID() + extension(file.getOriginalFilename());
        StorageService.StoredObject stored = write(key, file);

        ClaimDocument document = new ClaimDocument();
        document.setClaim(claim);
        document.setOriginalFilename(file.getOriginalFilename());
        document.setContentType(file.getContentType());
        document.setSizeBytes(stored.size());
        document.setStorageKey(stored.key());

        ClaimDocument saved = documents.save(document);
        events.publishEvent(new DocumentUploadedEvent(saved.getId()));
        return document(saved, null);
    }

    @Transactional
    public void deleteDocument(UUID claimId, UUID documentId) {
        ClaimDocument document = documents.findById(documentId)
                .filter(candidate -> candidate.getClaim().getId().equals(claimId))
                .orElseThrow(() -> new ClaimNotFoundException(documentId));
        storage.delete(document.getStorageKey());
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

    private StorageService.StoredObject write(String key, MultipartFile file) {
        try {
            return storage.store(key, file.getInputStream(), file.getSize(), file.getContentType());
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

    private static boolean hasText(String value) {
        return value != null && !value.isBlank();
    }

    private static String trimToNull(String value) {
        return hasText(value) ? value.trim() : null;
    }

    private static ClaimSummaryResponse summary(Claim claim) {
        return new ClaimSummaryResponse(
                claim.getId(),
                claim.getReference(),
                claim.getClaimantName(),
                claim.getStatus().name(),
                claim.getDocuments().size(),
                claim.getCreatedAt());
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
                docs);
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
