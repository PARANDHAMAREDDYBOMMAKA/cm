package com.claimguard.claim;

import com.claimguard.claim.dto.ClaimDetailResponse;
import com.claimguard.claim.dto.ClaimSummaryResponse;
import com.claimguard.claim.dto.DocumentResponse;
import com.claimguard.storage.StorageService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.List;
import java.util.UUID;

@Service
public class ClaimService {

    private final ClaimRepository claims;
    private final ClaimDocumentRepository documents;
    private final StorageService storage;

    public ClaimService(ClaimRepository claims, ClaimDocumentRepository documents, StorageService storage) {
        this.claims = claims;
        this.documents = documents;
        this.storage = storage;
    }

    @Transactional
    public ClaimDetailResponse create(String reference) {
        Claim claim = new Claim();
        claim.setReference(hasText(reference) ? reference.trim() : generateReference());
        claim.setStatus(ClaimStatus.RECEIVED);
        return detail(claims.save(claim));
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
        return document(documents.save(document));
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

    private static ClaimSummaryResponse summary(Claim claim) {
        return new ClaimSummaryResponse(
                claim.getId(),
                claim.getReference(),
                claim.getStatus().name(),
                claim.getDocuments().size(),
                claim.getCreatedAt());
    }

    private static ClaimDetailResponse detail(Claim claim) {
        List<DocumentResponse> docs = claim.getDocuments().stream().map(ClaimService::document).toList();
        return new ClaimDetailResponse(
                claim.getId(),
                claim.getReference(),
                claim.getStatus().name(),
                claim.getCreatedAt(),
                claim.getUpdatedAt(),
                docs);
    }

    private static DocumentResponse document(ClaimDocument document) {
        return new DocumentResponse(
                document.getId(),
                document.getOriginalFilename(),
                document.getContentType(),
                document.getSizeBytes(),
                document.getCreatedAt());
    }
}
