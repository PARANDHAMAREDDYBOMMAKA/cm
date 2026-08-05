package com.claimguard.metrics;

import com.claimguard.claim.Claim;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.UUID;

public interface MetricsRepository extends Repository<Claim, UUID> {

    String CLAIM_TOTALS = "select d.claim_id as claim_id, max(e.total_amount) as total "
            + "from document_extraction e "
            + "join claim_document d on d.id = e.document_id "
            + "where e.total_amount is not null "
            + "group by d.claim_id";

    @Query(value = "select count(*) from claim", nativeQuery = true)
    long countClaims();

    @Query(value = "select count(*) from claim_decision", nativeQuery = true)
    long countDecisions();

    @Query(value = "select outcome, count(*) from claim_decision group by outcome", nativeQuery = true)
    List<Object[]> countByOutcome();

    @Query(value = "select band, count(*) from claim_risk group by band", nativeQuery = true)
    List<Object[]> countByRiskBand();

    @Query(value = "select coalesce(sum(m.total), 0) from (" + CLAIM_TOTALS + ") m", nativeQuery = true)
    BigDecimal totalAmountProcessed();

    @Query(value = "select coalesce(sum(m.total), 0) from (" + CLAIM_TOTALS + ") m "
            + "join claim_decision cd on cd.claim_id = m.claim_id "
            + "where cd.outcome in (:outcomes)", nativeQuery = true)
    BigDecimal leakageCaught(@Param("outcomes") Collection<String> outcomes);

    @Query(value = "select avg(extract(epoch from (cd.decided_at - c.created_at)) / 60.0) "
            + "from claim_decision cd join claim c on c.id = cd.claim_id", nativeQuery = true)
    Double averageDecisionMinutes();

    @Query(value = "select count(*) from claim c "
            + "left join claim_decision cd on cd.claim_id = c.id "
            + "where cd.claim_id is null and c.created_at < :cutoff", nativeQuery = true)
    long countOpenBeyondSla(@Param("cutoff") Instant cutoff);
}
