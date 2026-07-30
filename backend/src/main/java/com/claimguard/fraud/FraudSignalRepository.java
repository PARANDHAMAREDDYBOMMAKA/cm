package com.claimguard.fraud;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

public interface FraudSignalRepository extends JpaRepository<FraudSignal, UUID> {

    List<FraudSignal> findByClaimIdOrderByWeightDesc(UUID claimId);

    List<FraudSignal> findByClaimIdIn(Collection<UUID> claimIds);

    @Query("select s.type, count(s) from FraudSignal s group by s.type order by count(s) desc")
    List<Object[]> countByType();

    void deleteByClaimId(UUID claimId);
}
