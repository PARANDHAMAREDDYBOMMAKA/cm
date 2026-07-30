package com.claimguard.extraction;

import com.claimguard.audit.AuditAction;
import com.claimguard.audit.AuditService;
import com.claimguard.claim.Claim;
import com.claimguard.claim.ClaimDocument;
import com.claimguard.claim.ClaimDocumentRepository;
import com.claimguard.claim.ClaimNotFoundException;
import com.claimguard.claim.ClaimRepository;
import com.claimguard.claim.ClaimStatus;
import com.claimguard.extraction.dto.ExtractionResponse;
import com.claimguard.fraud.ClaimAssessmentRequestedEvent;
import com.claimguard.support.Values;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@Service
public class ExtractionStore {

    private final DocumentExtractionRepository extractions;
    private final ClaimDocumentRepository documents;
    private final ClaimRepository claims;
    private final AuditService audit;
    private final ApplicationEventPublisher events;

    public ExtractionStore(DocumentExtractionRepository extractions,
            ClaimDocumentRepository documents,
            ClaimRepository claims,
            AuditService audit,
            ApplicationEventPublisher events) {
        this.extractions = extractions;
        this.documents = documents;
        this.claims = claims;
        this.audit = audit;
        this.events = events;
    }

    @Transactional
    public QueueOutcome queue(UUID documentId, boolean force) {
        ClaimDocument document = documents.findById(documentId)
                .orElseThrow(() -> new ClaimNotFoundException(documentId));
        DocumentExtraction extraction = extractions.findByDocumentId(documentId)
                .orElseGet(() -> {
                    DocumentExtraction created = new DocumentExtraction();
                    created.setDocument(document);
                    return created;
                });
        if (!force && extraction.getStatus() == ExtractionStatus.COMPLETED) {
            return new QueueOutcome(ExtractionMapper.toResponse(extraction), false);
        }
        extraction.setStatus(ExtractionStatus.PENDING);
        extraction.setError(null);
        return new QueueOutcome(ExtractionMapper.toResponse(extractions.save(extraction)), true);
    }

    @Transactional(readOnly = true)
    public List<UUID> findUnfinished() {
        return extractions.findByStatusIn(List.of(ExtractionStatus.PENDING, ExtractionStatus.RUNNING)).stream()
                .map(extraction -> extraction.getDocument().getId())
                .toList();
    }

    @Transactional
    public Optional<PendingJob> begin(UUID documentId) {
        Optional<DocumentExtraction> found = extractions.findByDocumentId(documentId);
        if (found.isEmpty()) {
            return Optional.empty();
        }
        DocumentExtraction extraction = found.get();
        if (extraction.getStatus() == ExtractionStatus.COMPLETED) {
            return Optional.empty();
        }
        extraction.setStatus(ExtractionStatus.RUNNING);
        extraction.setError(null);
        extractions.save(extraction);

        ClaimDocument document = extraction.getDocument();
        applyClaimStatus(document.getClaim());
        return Optional.of(new PendingJob(
                document.getStorageKey(),
                document.getContentType(),
                document.getOriginalFilename()));
    }

    @Transactional
    public void complete(UUID documentId, DocumentReader.ExtractedDocument result) {
        extractions.findByDocumentId(documentId).ifPresent(extraction -> {
            apply(extraction, result);
            extraction.setModel(Values.truncate(result.model(), 128));
            extraction.setStatus(ExtractionStatus.COMPLETED);
            extraction.setError(null);
            extractions.save(extraction);
            Claim claim = extraction.getDocument().getClaim();
            applyClaimStatus(claim);
            audit.record(claim.getId(), claim.getReference(), AuditAction.EXTRACTION_COMPLETED,
                    "Read " + extraction.getDocument().getOriginalFilename() + " and extracted its fields.",
                    Map.of("model", String.valueOf(extraction.getModel()),
                            "invoiceNumber", String.valueOf(extraction.getInvoiceNumber()),
                            "totalAmount", extraction.getTotalAmount() == null
                                    ? "null"
                                    : extraction.getTotalAmount().toPlainString()));
            events.publishEvent(new ClaimAssessmentRequestedEvent(claim.getId()));
        });
    }

    @Transactional
    public void fail(UUID documentId, ExtractionStatus status, String message) {
        extractions.findByDocumentId(documentId).ifPresent(extraction -> {
            extraction.setStatus(status);
            extraction.setError(Values.truncate(message, 2000));
            extractions.save(extraction);
            Claim claim = extraction.getDocument().getClaim();
            applyClaimStatus(claim);
            audit.record(claim.getId(), claim.getReference(), AuditAction.EXTRACTION_FAILED,
                    "Could not read " + extraction.getDocument().getOriginalFilename() + ".",
                    Map.of("status", status.name(), "error", String.valueOf(extraction.getError())));
        });
    }

    @Transactional(readOnly = true)
    public void requireDocumentInClaim(UUID claimId, UUID documentId) {
        requireDocument(claimId, documentId);
    }

    @Transactional(readOnly = true)
    public ExtractionResponse requireInClaim(UUID claimId, UUID documentId) {
        requireDocument(claimId, documentId);
        return ExtractionMapper.toResponse(extractions.findByDocumentId(documentId)
                .orElseThrow(() -> new ClaimNotFoundException(documentId)));
    }

    @Transactional
    public ExtractionResponse applyEdits(UUID claimId, UUID documentId, Map<String, String> fields) {
        requireDocument(claimId, documentId);
        DocumentExtraction extraction = extractions.findByDocumentId(documentId)
                .orElseThrow(() -> new ClaimNotFoundException(documentId));
        fields.forEach((field, value) -> ExtractionFieldWriter.write(extraction, field, value));
        Claim claim = extraction.getDocument().getClaim();
        audit.record(claim.getId(), claim.getReference(), AuditAction.EXTRACTION_EDITED,
                "A reviewer corrected " + fields.size() + " extracted field(s).",
                fields);
        return ExtractionMapper.toResponse(extractions.save(extraction));
    }

    private ClaimDocument requireDocument(UUID claimId, UUID documentId) {
        return documents.findById(documentId)
                .filter(document -> document.getClaim().getId().equals(claimId))
                .orElseThrow(() -> new ClaimNotFoundException(documentId));
    }

    private void apply(DocumentExtraction extraction, DocumentReader.ExtractedDocument result) {
        setUnlessEdited(extraction, ExtractionField.DOCUMENT_TYPE,
                () -> extraction.setDocumentType(Values.truncate(result.documentType(), 64)));
        setUnlessEdited(extraction, ExtractionField.PATIENT_NAME,
                () -> extraction.setPatientName(Values.truncate(result.patientName(), 255)));
        setUnlessEdited(extraction, ExtractionField.PATIENT_AGE,
                () -> extraction.setPatientAge(Values.truncate(result.patientAge(), 32)));
        setUnlessEdited(extraction, ExtractionField.PATIENT_GENDER,
                () -> extraction.setPatientGender(Values.truncate(result.patientGender(), 32)));
        setUnlessEdited(extraction, ExtractionField.PATIENT_ID,
                () -> extraction.setPatientId(Values.truncate(result.patientId(), 128)));
        setUnlessEdited(extraction, ExtractionField.PROVIDER_NAME,
                () -> extraction.setProviderName(Values.truncate(result.providerName(), 255)));
        setUnlessEdited(extraction, ExtractionField.PROVIDER_ADDRESS,
                () -> extraction.setProviderAddress(Values.truncate(result.providerAddress(), 512)));
        setUnlessEdited(extraction, ExtractionField.DIAGNOSIS,
                () -> extraction.setDiagnosis(Values.truncate(result.diagnosis(), 1024)));
        setUnlessEdited(extraction, ExtractionField.PROCEDURES,
                () -> extraction.setProcedures(new ArrayList<>(result.procedures())));
        setUnlessEdited(extraction, ExtractionField.ADMISSION_DATE,
                () -> extraction.setAdmissionDate(Values.date(result.admissionDate())));
        setUnlessEdited(extraction, ExtractionField.DISCHARGE_DATE,
                () -> extraction.setDischargeDate(Values.date(result.dischargeDate())));
        setUnlessEdited(extraction, ExtractionField.INVOICE_NUMBER,
                () -> extraction.setInvoiceNumber(Values.truncate(result.invoiceNumber(), 128)));
        setUnlessEdited(extraction, ExtractionField.INVOICE_DATE,
                () -> extraction.setInvoiceDate(Values.date(result.invoiceDate())));
        setUnlessEdited(extraction, ExtractionField.TOTAL_AMOUNT,
                () -> extraction.setTotalAmount(result.totalAmount()));
        setUnlessEdited(extraction, ExtractionField.CURRENCY,
                () -> extraction.setCurrency(Values.truncate(result.currency(), 8)));
        setUnlessEdited(extraction, ExtractionField.LINE_ITEMS,
                () -> extraction.replaceLineItems(toLineItems(result.lineItems())));

        extraction.setConfidence(mergeConfidence(extraction, result.confidence()));
        extraction.setRawResponse(result.raw());
    }

    private static void setUnlessEdited(DocumentExtraction extraction, String field, Runnable setter) {
        if (!extraction.wasEdited(field)) {
            setter.run();
        }
    }

    private static Map<String, Double> mergeConfidence(DocumentExtraction extraction, Map<String, Double> scores) {
        Map<String, Double> merged = new LinkedHashMap<>();
        if (scores != null) {
            merged.putAll(scores);
        }
        for (String field : ExtractionField.ALL) {
            if (extraction.wasEdited(field)) {
                merged.put(field, 1.0);
            }
        }
        return merged;
    }

    private static List<ExtractionLineItem> toLineItems(List<DocumentReader.LineItem> items) {
        List<ExtractionLineItem> entities = new ArrayList<>();
        for (DocumentReader.LineItem item : items) {
            ExtractionLineItem entity = new ExtractionLineItem();
            entity.setDescription(item.description());
            entity.setCode(item.code());
            entity.setQuantity(item.quantity());
            entity.setUnitAmount(item.unitAmount());
            entity.setAmount(item.amount());
            entities.add(entity);
        }
        return entities;
    }

    private void applyClaimStatus(Claim claim) {
        if (!claim.getStatus().isPipelineOwned()) {
            return;
        }
        List<ExtractionStatus> statuses = claim.getDocuments().stream()
                .map(document -> extractions.findByDocumentId(document.getId())
                        .map(DocumentExtraction::getStatus)
                        .orElse(ExtractionStatus.PENDING))
                .toList();

        ClaimStatus next;
        if (statuses.isEmpty()) {
            next = ClaimStatus.RECEIVED;
        } else if (statuses.contains(ExtractionStatus.PENDING) || statuses.contains(ExtractionStatus.RUNNING)) {
            next = ClaimStatus.PROCESSING;
        } else if (statuses.contains(ExtractionStatus.COMPLETED)) {
            next = ClaimStatus.EXTRACTED;
        } else if (statuses.contains(ExtractionStatus.FAILED)) {
            next = ClaimStatus.FAILED;
        } else {
            next = ClaimStatus.RECEIVED;
        }

        if (claim.getStatus() != next) {
            claim.setStatus(next);
            claims.save(claim);
        }
    }

    public record PendingJob(String storageKey, String contentType, String filename) {
    }

    public record QueueOutcome(ExtractionResponse response, boolean submitted) {
    }
}
