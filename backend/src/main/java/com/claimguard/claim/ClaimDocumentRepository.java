package com.claimguard.claim;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.UUID;

public interface ClaimDocumentRepository extends JpaRepository<ClaimDocument, UUID> {

    @Query("select d from ClaimDocument d where d.contentSha256 = :hash and d.claim.id <> :claimId")
    List<ClaimDocument> findDuplicatesByHash(@Param("hash") String hash, @Param("claimId") UUID claimId);

    @Query("select d from ClaimDocument d where d.perceptualHash is not null and d.claim.id <> :claimId")
    List<ClaimDocument> findHashedOutsideClaim(@Param("claimId") UUID claimId);

    @Query("select d from ClaimDocument d where d.deviceFingerprint = :fingerprint and d.claim.id <> :claimId")
    List<ClaimDocument> findByFingerprintOutsideClaim(@Param("fingerprint") String fingerprint,
            @Param("claimId") UUID claimId);
}
