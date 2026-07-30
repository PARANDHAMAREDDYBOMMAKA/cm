package com.claimguard.audit;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;

import java.util.Optional;

public interface AuditHeadRepository extends JpaRepository<AuditHead, String> {

    @Override
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<AuditHead> findById(String id);
}
