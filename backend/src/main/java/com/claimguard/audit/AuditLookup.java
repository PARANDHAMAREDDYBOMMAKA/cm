package com.claimguard.audit;

import com.claimguard.audit.dto.AuditEventResponse;
import com.claimguard.audit.dto.AuditVerificationResponse;
import com.claimguard.web.dto.PageResponse;
import org.springframework.data.domain.Limit;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Consumer;

@Service
public class AuditLookup {

    static final int BATCH_SIZE = 500;

    private final AuditEventRepository events;
    private final AuditHeadRepository heads;
    private final AuditCheckpointRepository checkpoints;

    public AuditLookup(AuditEventRepository events,
            AuditHeadRepository heads,
            AuditCheckpointRepository checkpoints) {
        this.events = events;
        this.heads = heads;
        this.checkpoints = checkpoints;
    }

    @Transactional(readOnly = true)
    public List<AuditEventResponse> forClaim(UUID claimId) {
        return events.findByClaimIdOrderBySeqAsc(claimId).stream().map(AuditLookup::toResponse).toList();
    }

    @Transactional(readOnly = true)
    public List<AuditEventResponse> recent(int limit) {
        return events.findAllByOrderBySeqDesc(Limit.of(limit)).stream().map(AuditLookup::toResponse).toList();
    }

    @Transactional(readOnly = true)
    public PageResponse<AuditEventResponse> page(int page, int size) {
        return PageResponse.of(events.findAllByOrderBySeqDesc(PageRequest.of(page, size)), AuditLookup::toResponse);
    }

    @Transactional(readOnly = true)
    public void stream(Consumer<AuditEventResponse> consumer) {
        long after = 0;
        while (true) {
            List<AuditEvent> batch = events.findBySeqGreaterThanOrderBySeqAsc(after, Limit.of(BATCH_SIZE));
            if (batch.isEmpty()) {
                return;
            }
            for (AuditEvent event : batch) {
                consumer.accept(toResponse(event));
            }
            after = batch.get(batch.size() - 1).getSeq();
        }
    }

    @Transactional
    public AuditVerificationResponse verify() {
        return check(true);
    }

    @Transactional(readOnly = true)
    public AuditVerificationResponse summary() {
        return check(false);
    }

    private AuditVerificationResponse check(boolean advance) {
        AuditCheckpoint checkpoint = checkpoints.findById(AuditCheckpoint.ID).orElseGet(AuditLookup::genesis);
        long total = events.count();
        if (!checkpoint.isIntact()) {
            return new AuditVerificationResponse(false, total, checkpoint.getBrokenSeq(), null,
                    checkpoint.getDetail());
        }

        String previous = checkpoint.getVerifiedHash();
        long verifiedSeq = checkpoint.getVerifiedSeq();
        long expectedSeq = verifiedSeq + 1;

        while (true) {
            List<AuditEvent> batch = events.findBySeqGreaterThanOrderBySeqAsc(verifiedSeq, Limit.of(BATCH_SIZE));
            if (batch.isEmpty()) {
                break;
            }
            for (AuditEvent event : batch) {
                if (event.getSeq() != expectedSeq) {
                    return broken(advance, checkpoint, total, event.getSeq(),
                            "Sequence " + expectedSeq + " is missing; the next stored entry is "
                                    + event.getSeq() + ".");
                }
                if (!previous.equals(event.getPreviousHash())) {
                    return broken(advance, checkpoint, total, event.getSeq(),
                            "Entry " + event.getSeq() + " does not link to the entry before it.");
                }
                if (!AuditSeal.seal(event).equals(event.getHash())) {
                    return broken(advance, checkpoint, total, event.getSeq(),
                            "Entry " + event.getSeq() + " has been altered since it was written.");
                }
                previous = event.getHash();
                verifiedSeq = event.getSeq();
                expectedSeq++;
            }
        }

        String headHash = heads.findById(AuditHead.ID).map(AuditHead::getHash).orElse(AuditSeal.GENESIS_HASH);
        if (!previous.equals(headHash)) {
            return broken(advance, checkpoint, total, null,
                    "The chain head does not match the last recorded entry.");
        }

        if (advance) {
            checkpoint.setVerifiedSeq(verifiedSeq);
            checkpoint.setVerifiedHash(previous);
            checkpoint.setIntact(true);
            checkpoint.setBrokenSeq(null);
            checkpoint.setDetail(message(total));
            checkpoint.setVerifiedAt(Instant.now());
            checkpoints.save(checkpoint);
        }
        return new AuditVerificationResponse(true, total, null, headHash, message(total));
    }

    private AuditVerificationResponse broken(boolean advance,
            AuditCheckpoint checkpoint,
            long total,
            Long seq,
            String detail) {
        if (advance) {
            checkpoint.setIntact(false);
            checkpoint.setBrokenSeq(seq);
            checkpoint.setDetail(detail);
            checkpoint.setVerifiedAt(Instant.now());
            checkpoints.save(checkpoint);
        }
        return new AuditVerificationResponse(false, total, seq, null, detail);
    }

    private static String message(long total) {
        return total == 0 ? "No audit entries yet." : "All " + total + " entries verify against the seal.";
    }

    private static AuditCheckpoint genesis() {
        AuditCheckpoint checkpoint = new AuditCheckpoint();
        checkpoint.setId(AuditCheckpoint.ID);
        checkpoint.setVerifiedSeq(0);
        checkpoint.setVerifiedHash(AuditSeal.GENESIS_HASH);
        checkpoint.setIntact(true);
        checkpoint.setDetail("No audit entries verified yet.");
        checkpoint.setVerifiedAt(Instant.now());
        return checkpoint;
    }

    private static AuditEventResponse toResponse(AuditEvent event) {
        return new AuditEventResponse(
                event.getSeq(),
                event.getId(),
                event.getClaimId(),
                event.getClaimReference(),
                event.getActor(),
                event.getAction().name(),
                event.getSummary(),
                event.getDetails() == null ? Map.of() : event.getDetails(),
                event.getPreviousHash(),
                event.getHash(),
                event.getCreatedAt());
    }
}
