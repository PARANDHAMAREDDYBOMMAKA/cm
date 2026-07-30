package com.claimguard.audit;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

@Entity
@Table(name = "audit_event")
@Getter
@Setter
public class AuditEvent {

    @Id
    private long seq;

    @Column(nullable = false, unique = true)
    private UUID id;

    @Column(name = "claim_id")
    private UUID claimId;

    @Column(name = "claim_reference", length = 64)
    private String claimReference;

    @Column(nullable = false, length = 255)
    private String actor;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 64)
    private AuditAction action;

    @Column(nullable = false, length = 1000)
    private String summary;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "jsonb")
    private Map<String, String> details = new LinkedHashMap<>();

    @Column(name = "previous_hash", nullable = false, length = 64)
    private String previousHash;

    @Column(nullable = false, length = 64)
    private String hash;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;
}
