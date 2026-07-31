package com.claimguard.audit;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface AuditHeadRepository extends JpaRepository<AuditHead, String> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select head from AuditHead head where head.id = :id")
    Optional<AuditHead> findAndLock(@Param("id") String id);
}
