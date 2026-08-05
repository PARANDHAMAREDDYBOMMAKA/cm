package com.claimguard.fraud;

import com.claimguard.claim.ClaimDocument;
import com.claimguard.claim.ClaimDocumentRepository;
import com.claimguard.extraction.MimeTypes;
import com.claimguard.extraction.PdfRasterizer;
import com.claimguard.storage.StorageService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.io.InputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.HexFormat;
import java.util.UUID;

@Component
public class DocumentIntake {

    private static final Logger log = LoggerFactory.getLogger(DocumentIntake.class);

    private final MetadataInspector inspector;
    private final DocumentForensicsRepository forensics;
    private final ClaimDocumentRepository documents;
    private final PdfRasterizer rasterizer;
    private final StorageService storage;

    public DocumentIntake(MetadataInspector inspector,
            DocumentForensicsRepository forensics,
            ClaimDocumentRepository documents,
            PdfRasterizer rasterizer,
            StorageService storage) {
        this.inspector = inspector;
        this.forensics = forensics;
        this.documents = documents;
        this.rasterizer = rasterizer;
        this.storage = storage;
    }

    public void applyContentHash(ClaimDocument document, byte[] content) {
        document.setContentSha256(sha256(content));
    }

    @Transactional
    public void fingerprint(UUID documentId) {
        documents.findById(documentId).ifPresent(document -> {
            if (document.getFingerprintedAt() != null && forensics.findByDocumentId(documentId).isPresent()) {
                return;
            }
            byte[] content = download(document);
            if (content == null) {
                return;
            }
            applyFingerprints(document, content);
            recordForensics(document, content);
            documents.save(document);
        });
    }

    public void backfill(ClaimDocument document) {
        boolean needsFingerprints = document.getFingerprintedAt() == null;
        boolean needsForensics = forensics.findByDocumentId(document.getId()).isEmpty();
        if (!needsFingerprints && !needsForensics) {
            return;
        }
        byte[] content = download(document);
        if (content == null) {
            return;
        }
        if (needsFingerprints) {
            applyFingerprints(document, content);
        }
        if (needsForensics) {
            recordForensics(document, content);
        }
    }

    private byte[] download(ClaimDocument document) {
        try (InputStream stream = storage.retrieve(document.getStorageKey()).content()) {
            return stream.readAllBytes();
        } catch (IOException | RuntimeException exception) {
            log.warn("Could not download {} for backfill: {}", document.getStorageKey(), exception.getMessage());
            return null;
        }
    }

    public void applyFingerprints(ClaimDocument document, byte[] content) {
        if (document.getContentSha256() == null) {
            document.setContentSha256(sha256(content));
        }
        document.setPerceptualHash(perceptualHash(content, document.getContentType()));
        document.setFingerprintedAt(Instant.now());
    }

    public void recordForensics(ClaimDocument document, byte[] content) {
        ForensicsReport report = inspector.inspect(content, document.getContentType());
        DocumentForensics entity = forensics.findByDocumentId(document.getId())
                .orElseGet(DocumentForensics::new);
        entity.setDocumentId(document.getId());
        entity.setHasMetadata(report.hasMetadata());
        entity.setSoftware(report.software());
        entity.setCameraMake(report.cameraMake());
        entity.setCameraModel(report.cameraModel());
        entity.setSourceCreatedAt(report.createdAt());
        entity.setSourceModifiedAt(report.modifiedAt());
        entity.setProducer(report.producer());
        entity.getAttributes().clear();
        entity.getAttributes().putAll(report.attributes());
        forensics.save(entity);
    }

    private Long perceptualHash(byte[] content, String contentType) {
        String type = MimeTypes.normalize(contentType);
        try {
            if (type.startsWith("image/")) {
                return PerceptualHash.of(content);
            }
            if ("application/pdf".equals(type)) {
                return PerceptualHash.of(rasterizer.rasterize(content).image());
            }
        } catch (RuntimeException exception) {
            log.warn("Could not compute perceptual hash: {}", exception.getMessage());
        }
        return null;
    }

    private static String sha256(byte[] content) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(content));
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 is unavailable", exception);
        }
    }
}
