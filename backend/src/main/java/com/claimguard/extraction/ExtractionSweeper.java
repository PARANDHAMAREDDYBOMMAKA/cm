package com.claimguard.extraction;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.UUID;

@Component
public class ExtractionSweeper {

    private static final Logger log = LoggerFactory.getLogger(ExtractionSweeper.class);

    private final ExtractionStore store;
    private final ExtractionQueue queue;

    public ExtractionSweeper(ExtractionStore store, ExtractionQueue queue) {
        this.store = store;
        this.queue = queue;
    }

    @Scheduled(fixedDelayString = "${EXTRACTION_SWEEP_INTERVAL_MS:60000}",
            initialDelayString = "${EXTRACTION_SWEEP_INITIAL_DELAY_MS:60000}")
    public void sweep() {
        try {
            resubmit(store.reclaimStalled(false), "stalled");
            resubmit(store.reclaimRetryable(), "failed");
        } catch (RuntimeException exception) {
            log.warn("Extraction sweep failed: {}", exception.getMessage());
        }
    }

    private void resubmit(List<UUID> documentIds, String reason) {
        if (documentIds.isEmpty()) {
            return;
        }
        log.info("Requeuing {} {} extraction(s)", documentIds.size(), reason);
        documentIds.forEach(queue::submit);
    }
}
