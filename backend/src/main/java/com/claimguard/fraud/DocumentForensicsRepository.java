package com.claimguard.fraud;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface DocumentForensicsRepository extends JpaRepository<DocumentForensics, UUID> {

    Optional<DocumentForensics> findByDocumentId(UUID documentId);
}
