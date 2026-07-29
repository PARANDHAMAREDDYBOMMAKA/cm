package com.claimguard.fraud;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

public interface ClaimRiskRepository extends JpaRepository<ClaimRisk, UUID> {

    List<ClaimRisk> findByClaimIdIn(Collection<UUID> claimIds);
}
