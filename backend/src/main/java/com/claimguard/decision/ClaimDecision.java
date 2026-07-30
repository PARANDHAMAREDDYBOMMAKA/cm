package com.claimguard.decision;

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
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "claim_decision")
@Getter
@Setter
public class ClaimDecision {

    @Id
    @Column(name = "claim_id")
    private UUID claimId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private DecisionOutcome outcome;

    @Column(nullable = false)
    private boolean automatic;

    @Column(name = "decided_by", nullable = false, length = 255)
    private String decidedBy;

    @Column(length = 2000)
    private String note;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "jsonb")
    private List<String> reasons = new ArrayList<>();

    @Column(name = "decided_at", nullable = false)
    private Instant decidedAt;
}
