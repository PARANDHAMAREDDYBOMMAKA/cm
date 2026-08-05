package com.claimguard.extraction;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface DocumentExtractionRepository extends JpaRepository<DocumentExtraction, UUID> {

    Optional<DocumentExtraction> findByDocumentId(UUID documentId);

    List<DocumentExtraction> findByDocumentIdIn(Collection<UUID> documentIds);

    List<DocumentExtraction> findByStatusIn(Collection<ExtractionStatus> statuses);

    List<DocumentExtraction> findByInvoiceNumberIgnoreCase(String invoiceNumber);

    @Query("select e from DocumentExtraction e where "
            + "(e.status = com.claimguard.extraction.ExtractionStatus.PENDING and e.updatedAt < :staleBefore) "
            + "or (e.status = com.claimguard.extraction.ExtractionStatus.RUNNING "
            + "and (e.leaseExpiresAt is null or e.leaseExpiresAt < :now))")
    List<DocumentExtraction> findStalled(@Param("now") Instant now, @Param("staleBefore") Instant staleBefore);

    @Query("select e from DocumentExtraction e where e.status = com.claimguard.extraction.ExtractionStatus.FAILED "
            + "and e.attempts < :maxAttempts and (e.nextAttemptAt is null or e.nextAttemptAt <= :now)")
    List<DocumentExtraction> findRetryable(@Param("maxAttempts") int maxAttempts, @Param("now") Instant now);

    @Query("select e.document.claim.id, max(e.totalAmount) from DocumentExtraction e "
            + "where e.totalAmount is not null group by e.document.claim.id")
    List<Object[]> maxTotalByClaim();
}
