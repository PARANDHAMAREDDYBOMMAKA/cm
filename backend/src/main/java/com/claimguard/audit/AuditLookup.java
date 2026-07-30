package com.claimguard.audit;

import com.claimguard.audit.dto.AuditEventResponse;
import com.claimguard.audit.dto.AuditVerificationResponse;
import org.springframework.data.domain.Limit;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
public class AuditLookup {

    private final AuditEventRepository events;
    private final AuditHeadRepository heads;

    public AuditLookup(AuditEventRepository events, AuditHeadRepository heads) {
        this.events = events;
        this.heads = heads;
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
    public List<AuditEventResponse> all() {
        return events.findAllByOrderBySeqAsc().stream().map(AuditLookup::toResponse).toList();
    }

    @Transactional(readOnly = true)
    public AuditVerificationResponse verify() {
        List<AuditEvent> chain = events.findAllByOrderBySeqAsc();
        String previous = AuditSeal.GENESIS_HASH;
        long expectedSeq = 1;

        for (AuditEvent event : chain) {
            if (event.getSeq() != expectedSeq) {
                return broken(chain.size(), event.getSeq(),
                        "Sequence " + expectedSeq + " is missing; the next stored entry is " + event.getSeq() + ".");
            }
            if (!previous.equals(event.getPreviousHash())) {
                return broken(chain.size(), event.getSeq(),
                        "Entry " + event.getSeq() + " does not link to the entry before it.");
            }
            if (!AuditSeal.seal(event).equals(event.getHash())) {
                return broken(chain.size(), event.getSeq(),
                        "Entry " + event.getSeq() + " has been altered since it was written.");
            }
            previous = event.getHash();
            expectedSeq++;
        }

        String headHash = heads.findById(AuditHead.ID).map(AuditHead::getHash).orElse(AuditSeal.GENESIS_HASH);
        if (!previous.equals(headHash)) {
            return broken(chain.size(), null, "The chain head does not match the last recorded entry.");
        }
        return new AuditVerificationResponse(true, chain.size(), null, headHash,
                chain.isEmpty() ? "No audit entries yet." : "All " + chain.size() + " entries verify against the seal.");
    }

    private static AuditVerificationResponse broken(long count, Long seq, String message) {
        return new AuditVerificationResponse(false, count, seq, null, message);
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
