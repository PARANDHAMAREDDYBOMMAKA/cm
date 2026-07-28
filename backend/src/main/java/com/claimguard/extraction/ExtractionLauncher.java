package com.claimguard.extraction;

import com.claimguard.extraction.dto.ExtractionResponse;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
public class ExtractionLauncher {

    private final ExtractionStore store;
    private final ExtractionQueue queue;

    public ExtractionLauncher(ExtractionStore store, ExtractionQueue queue) {
        this.store = store;
        this.queue = queue;
    }

    public ExtractionResponse launch(UUID documentId) {
        return launch(documentId, false);
    }

    public ExtractionResponse launch(UUID documentId, boolean force) {
        ExtractionStore.QueueOutcome outcome = store.queue(documentId, force);
        if (outcome.submitted()) {
            queue.submit(documentId);
        }
        return outcome.response();
    }
}
