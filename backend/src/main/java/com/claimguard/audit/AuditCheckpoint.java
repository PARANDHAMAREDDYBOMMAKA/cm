package com.claimguard.audit;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;

@Entity
@Table(name = "audit_checkpoint")
@Getter
@Setter
public class AuditCheckpoint {

    public static final String ID = "GLOBAL";

    @Id
    @Column(length = 16)
    private String id;

    @Column(name = "verified_seq", nullable = false)
    private long verifiedSeq;

    @Column(name = "verified_hash", nullable = false, length = 64)
    private String verifiedHash;

    @Column(nullable = false)
    private boolean intact;

    @Column(length = 1000)
    private String detail;

    @Column(name = "broken_seq")
    private Long brokenSeq;

    @Column(name = "verified_at", nullable = false)
    private Instant verifiedAt;
}
