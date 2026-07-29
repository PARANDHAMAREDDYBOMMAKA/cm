package com.claimguard.fraud;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "claim_risk")
@Getter
@Setter
public class ClaimRisk {

    @Id
    @Column(name = "claim_id")
    private UUID claimId;

    @Column(nullable = false)
    private int score;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private RiskBand band;

    @Column(name = "signal_count", nullable = false)
    private int signalCount;

    @Column(name = "assessed_at", nullable = false)
    private Instant assessedAt;
}
