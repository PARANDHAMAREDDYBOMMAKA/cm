package com.claimguard.audit;

import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class AuditSealTest {

    private static AuditEvent event() {
        AuditEvent event = new AuditEvent();
        event.setSeq(1);
        event.setId(UUID.fromString("00000000-0000-0000-0000-000000000001"));
        event.setClaimId(UUID.fromString("00000000-0000-0000-0000-000000000002"));
        event.setActor("reviewer@example.com");
        event.setAction(AuditAction.DECISION_RECORDED);
        event.setSummary("Auto-approved: nothing of concern was found.");
        event.setDetails(new LinkedHashMap<>(Map.of("outcome", "AUTO_APPROVED", "riskScore", "0")));
        event.setPreviousHash(AuditSeal.GENESIS_HASH);
        event.setCreatedAt(Instant.parse("2026-07-31T10:15:30Z"));
        return event;
    }

    @Test
    void sealIsStableForTheSameContent() {
        assertThat(AuditSeal.seal(event())).isEqualTo(AuditSeal.seal(event()));
    }

    @Test
    void sealDoesNotDependOnDetailOrdering() {
        AuditEvent reordered = event();
        Map<String, String> flipped = new LinkedHashMap<>();
        flipped.put("riskScore", "0");
        flipped.put("outcome", "AUTO_APPROVED");
        reordered.setDetails(flipped);

        assertThat(AuditSeal.seal(reordered)).isEqualTo(AuditSeal.seal(event()));
    }

    @Test
    void changingTheSummaryChangesTheSeal() {
        AuditEvent tampered = event();
        tampered.setSummary("Auto-approved: nothing of concern was found");

        assertThat(AuditSeal.seal(tampered)).isNotEqualTo(AuditSeal.seal(event()));
    }

    @Test
    void changingADetailChangesTheSeal() {
        AuditEvent tampered = event();
        tampered.setDetails(new LinkedHashMap<>(Map.of("outcome", "AUTO_APPROVED", "riskScore", "90")));

        assertThat(AuditSeal.seal(tampered)).isNotEqualTo(AuditSeal.seal(event()));
    }

    @Test
    void relinkingToADifferentPredecessorChangesTheSeal() {
        AuditEvent relinked = event();
        relinked.setPreviousHash("1".repeat(64));

        assertThat(AuditSeal.seal(relinked)).isNotEqualTo(AuditSeal.seal(event()));
    }

    @Test
    void sealIsAHexSha256() {
        assertThat(AuditSeal.seal(event())).hasSize(64).matches("[0-9a-f]{64}");
    }
}
