package com.claimguard.fraud;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

public interface FraudSignalRepository extends JpaRepository<FraudSignal, UUID> {

    List<FraudSignal> findByClaimIdOrderByWeightDesc(UUID claimId);

    List<FraudSignal> findByClaimIdIn(Collection<UUID> claimIds);

    void deleteByClaimId(UUID claimId);
}
