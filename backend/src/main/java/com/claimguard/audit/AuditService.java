package com.claimguard.audit;

import com.claimguard.support.Values;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

@Service
public class AuditService {

    private static final Logger log = LoggerFactory.getLogger(AuditService.class);

    private final AuditEventRepository events;
    private final AuditHeadRepository heads;

    public AuditService(AuditEventRepository events, AuditHeadRepository heads) {
        this.events = events;
        this.heads = heads;
    }

    @Transactional
    public void record(UUID claimId, String claimReference, AuditAction action, String summary) {
        record(claimId, claimReference, action, summary, Map.of());
    }

    @Transactional
    public void record(UUID claimId,
            String claimReference,
            AuditAction action,
            String summary,
            Map<String, String> details) {
        try {
            append(claimId, claimReference, action, summary, details);
        } catch (RuntimeException exception) {
            log.warn("Could not write audit event {} for claim {}: {}", action, claimId, exception.getMessage());
        }
    }

    private void append(UUID claimId,
            String claimReference,
            AuditAction action,
            String summary,
            Map<String, String> details) {
        AuditHead head = heads.findAndLock(AuditHead.ID).orElseGet(AuditService::genesis);

        AuditEvent event = new AuditEvent();
        event.setSeq(head.getSeq() + 1);
        event.setId(UUID.randomUUID());
        event.setClaimId(claimId);
        event.setClaimReference(Values.truncate(claimReference, 64));
        event.setActor(Values.truncate(Actors.current(), 255));
        event.setAction(action);
        event.setSummary(Values.truncate(summary, 1000));
        event.setDetails(clean(details));
        event.setPreviousHash(head.getHash());
        event.setCreatedAt(Instant.now());
        event.setHash(AuditSeal.seal(event));
        events.save(event);

        head.setSeq(event.getSeq());
        head.setHash(event.getHash());
        heads.save(head);
    }

    private static AuditHead genesis() {
        AuditHead head = new AuditHead();
        head.setId(AuditHead.ID);
        head.setSeq(0);
        head.setHash(AuditSeal.GENESIS_HASH);
        return head;
    }

    private static Map<String, String> clean(Map<String, String> details) {
        Map<String, String> cleaned = new LinkedHashMap<>();
        if (details == null) {
            return cleaned;
        }
        details.forEach((key, value) -> {
            if (key != null && value != null) {
                cleaned.put(key, Values.truncate(value, 500));
            }
        });
        return cleaned;
    }
}
