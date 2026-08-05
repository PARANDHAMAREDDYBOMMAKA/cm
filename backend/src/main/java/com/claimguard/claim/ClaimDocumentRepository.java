package com.claimguard.claim;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

public interface ClaimDocumentRepository extends JpaRepository<ClaimDocument, UUID> {

    @Query("select d from ClaimDocument d where d.contentSha256 = :hash and d.id <> :documentId")
    List<ClaimDocument> findDuplicatesByHash(@Param("hash") String hash, @Param("documentId") UUID documentId);

    @Query(value = "select d.id, bit_count(cast(d.perceptual_hash # :hash as bit(64))) as distance "
            + "from claim_document d "
            + "where d.id <> :documentId and d.perceptual_hash is not null "
            + "and bit_count(cast(d.perceptual_hash # :hash as bit(64))) <= :maxDistance "
            + "order by distance asc limit 1", nativeQuery = true)
    List<Object[]> findNearestByPerceptualHash(@Param("hash") long hash,
            @Param("documentId") UUID documentId,
            @Param("maxDistance") int maxDistance);

    @Query("select d from ClaimDocument d where d.deviceFingerprint = :fingerprint and d.claim.id <> :claimId")
    List<ClaimDocument> findByFingerprintOutsideClaim(@Param("fingerprint") String fingerprint,
            @Param("claimId") UUID claimId);

    @Query("select d.claim.id, count(d) from ClaimDocument d where d.claim.id in :claimIds group by d.claim.id")
    List<Object[]> countByClaimIds(@Param("claimIds") Collection<UUID> claimIds);

    long countByClaimId(UUID claimId);
}
