package com.claimguard.audit;

import org.springframework.data.domain.Limit;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface AuditEventRepository extends JpaRepository<AuditEvent, Long> {

    List<AuditEvent> findByClaimIdOrderBySeqAsc(UUID claimId);

    List<AuditEvent> findAllByOrderBySeqDesc(Limit limit);

    Page<AuditEvent> findAllByOrderBySeqDesc(Pageable pageable);

    List<AuditEvent> findBySeqGreaterThanOrderBySeqAsc(long seq, Limit limit);
}
