package com.claimguard.extraction;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface DocumentExtractionRepository extends JpaRepository<DocumentExtraction, UUID> {

    Optional<DocumentExtraction> findByDocumentId(UUID documentId);

    List<DocumentExtraction> findByDocumentIdIn(Collection<UUID> documentIds);

    List<DocumentExtraction> findByStatusIn(Collection<ExtractionStatus> statuses);

    List<DocumentExtraction> findByInvoiceNumberIgnoreCase(String invoiceNumber);
}
