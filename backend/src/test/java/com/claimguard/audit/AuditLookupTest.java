package com.claimguard.audit;

import com.claimguard.audit.dto.AuditVerificationResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.data.domain.Limit;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class AuditLookupTest {

    @Mock
    private AuditEventRepository events;
    @Mock
    private AuditHeadRepository heads;
    @Mock
    private AuditCheckpointRepository checkpoints;

    private AuditLookup lookup;
    private List<AuditEvent> chain;

    @BeforeEach
    void setUp() {
        lookup = new AuditLookup(events, heads, checkpoints);
        chain = new ArrayList<>();

        when(events.count()).thenAnswer(invocation -> (long) chain.size());
        when(events.findBySeqGreaterThanOrderBySeqAsc(anyLong(), any(Limit.class))).thenAnswer(invocation -> {
            long after = invocation.getArgument(0);
            return chain.stream().filter(event -> event.getSeq() > after).toList();
        });
        when(checkpoints.findById(AuditCheckpoint.ID)).thenReturn(Optional.empty());
        when(checkpoints.save(any(AuditCheckpoint.class))).thenAnswer(invocation -> invocation.getArgument(0));
    }

    private void append(String summary) {
        AuditEvent event = new AuditEvent();
        event.setSeq(chain.size() + 1);
        event.setId(UUID.randomUUID());
        event.setActor("tester");
        event.setAction(AuditAction.CLAIM_CREATED);
        event.setSummary(summary);
        event.setDetails(Map.of());
        event.setCreatedAt(Instant.parse("2026-01-01T00:00:00Z").plusSeconds(chain.size()));
        event.setPreviousHash(chain.isEmpty() ? AuditSeal.GENESIS_HASH : chain.get(chain.size() - 1).getHash());
        event.setHash(AuditSeal.seal(event));
        chain.add(event);
        headIs(event.getHash());
    }

    private void headIs(String hash) {
        AuditHead head = new AuditHead();
        head.setId(AuditHead.ID);
        head.setSeq(chain.size());
        head.setHash(hash);
        when(heads.findById(AuditHead.ID)).thenReturn(Optional.of(head));
    }

    private void checkpointAt(long seq, String hash, boolean intact) {
        AuditCheckpoint checkpoint = new AuditCheckpoint();
        checkpoint.setId(AuditCheckpoint.ID);
        checkpoint.setVerifiedSeq(seq);
        checkpoint.setVerifiedHash(hash);
        checkpoint.setIntact(intact);
        checkpoint.setDetail(intact ? "ok" : "Entry 2 has been altered since it was written.");
        checkpoint.setBrokenSeq(intact ? null : 2L);
        checkpoint.setVerifiedAt(Instant.now());
        when(checkpoints.findById(AuditCheckpoint.ID)).thenReturn(Optional.of(checkpoint));
    }

    @Test
    void anEmptyChainIsIntact() {
        headIs(AuditSeal.GENESIS_HASH);

        AuditVerificationResponse result = lookup.verify();

        assertThat(result.intact()).isTrue();
        assertThat(result.eventCount()).isZero();
    }

    @Test
    void aWellFormedChainVerifiesAndAdvancesTheCheckpoint() {
        append("one");
        append("two");
        append("three");

        AuditVerificationResponse result = lookup.verify();

        assertThat(result.intact()).isTrue();
        assertThat(result.eventCount()).isEqualTo(3);

        org.mockito.ArgumentCaptor<AuditCheckpoint> captor =
                org.mockito.ArgumentCaptor.forClass(AuditCheckpoint.class);
        verify(checkpoints).save(captor.capture());
        assertThat(captor.getValue().getVerifiedSeq()).isEqualTo(3);
        assertThat(captor.getValue().getVerifiedHash()).isEqualTo(chain.get(2).getHash());
        assertThat(captor.getValue().isIntact()).isTrue();
    }

    @Test
    void anAlteredEntryIsDetectedAndLatchedIntoTheCheckpoint() {
        append("one");
        append("two");
        chain.get(1).setSummary("two, but edited in the database");

        AuditVerificationResponse result = lookup.verify();

        assertThat(result.intact()).isFalse();
        assertThat(result.brokenAtSeq()).isEqualTo(2);
        assertThat(result.message()).contains("has been altered");

        org.mockito.ArgumentCaptor<AuditCheckpoint> captor =
                org.mockito.ArgumentCaptor.forClass(AuditCheckpoint.class);
        verify(checkpoints).save(captor.capture());
        assertThat(captor.getValue().isIntact()).isFalse();
        assertThat(captor.getValue().getBrokenSeq()).isEqualTo(2);
    }

    @Test
    void aMissingSequenceIsDetected() {
        append("one");
        append("two");
        chain.remove(0);

        AuditVerificationResponse result = lookup.verify();

        assertThat(result.intact()).isFalse();
        assertThat(result.message()).contains("Sequence 1 is missing");
    }

    @Test
    void aHeadThatDoesNotMatchTheLastEntryIsDetected() {
        append("one");
        headIs("f".repeat(64));

        AuditVerificationResponse result = lookup.verify();

        assertThat(result.intact()).isFalse();
        assertThat(result.message()).contains("chain head does not match");
    }

    @Test
    void verificationResumesFromTheCheckpointInsteadOfRehashingTheWholeLog() {
        append("one");
        append("two");
        append("three");
        checkpointAt(2, chain.get(1).getHash(), true);

        AuditVerificationResponse result = lookup.verify();

        assertThat(result.intact()).isTrue();
        verify(events).findBySeqGreaterThanOrderBySeqAsc(org.mockito.ArgumentMatchers.eq(2L), any(Limit.class));
        verify(events, never()).findBySeqGreaterThanOrderBySeqAsc(org.mockito.ArgumentMatchers.eq(0L),
                any(Limit.class));
    }

    @Test
    void aBrokenCheckpointIsReportedWithoutReverifying() {
        append("one");
        append("two");
        checkpointAt(1, chain.get(0).getHash(), false);

        AuditVerificationResponse result = lookup.verify();

        assertThat(result.intact()).isFalse();
        assertThat(result.brokenAtSeq()).isEqualTo(2);
        verify(events, never()).findBySeqGreaterThanOrderBySeqAsc(anyLong(), any(Limit.class));
    }

    @Test
    void summaryReportsTheSameResultWithoutWritingACheckpoint() {
        append("one");
        append("two");

        AuditVerificationResponse result = lookup.summary();

        assertThat(result.intact()).isTrue();
        assertThat(result.eventCount()).isEqualTo(2);
        verify(checkpoints, never()).save(any(AuditCheckpoint.class));
    }
}
