package com.claimguard.audit;

import org.springframework.data.jpa.repository.JpaRepository;

public interface AuditCheckpointRepository extends JpaRepository<AuditCheckpoint, String> {
}
