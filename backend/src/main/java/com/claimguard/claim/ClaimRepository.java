package com.claimguard.claim;

import com.claimguard.decision.DecisionOutcome;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.Optional;
import java.util.UUID;

public interface ClaimRepository extends JpaRepository<Claim, UUID> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select c from Claim c where c.id = :id")
    Optional<Claim> findAndLock(@Param("id") UUID id);

    @Query("select c from Claim c where "
            + "(:subject = '' or c.ownerSubject is null or c.ownerSubject = :subject) and "
            + "(:organisation = '' or c.ownerOrg is null or c.ownerOrg = :organisation)")
    Page<Claim> findVisible(@Param("subject") String subject,
            @Param("organisation") String organisation,
            Pageable pageable);

    @Query("select c from Claim c where "
            + "(:subject = '' or c.ownerSubject is null or c.ownerSubject = :subject) and "
            + "(:organisation = '' or c.ownerOrg is null or c.ownerOrg = :organisation) and "
            + "(c.status in :statuses or c.id in "
            + "(select d.claimId from ClaimDecision d where d.outcome in :outcomes))")
    Page<Claim> findReviewQueue(@Param("subject") String subject,
            @Param("organisation") String organisation,
            @Param("statuses") Collection<ClaimStatus> statuses,
            @Param("outcomes") Collection<DecisionOutcome> outcomes,
            Pageable pageable);

    boolean existsByReferenceIgnoreCase(String reference);

    long countByOwnerSubjectIsNull();
}
