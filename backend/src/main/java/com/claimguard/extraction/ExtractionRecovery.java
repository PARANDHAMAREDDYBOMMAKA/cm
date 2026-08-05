package com.claimguard.extraction;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.UUID;

@Component
public class ExtractionRecovery implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(ExtractionRecovery.class);

    private final ExtractionStore store;
    private final ExtractionQueue queue;

    public ExtractionRecovery(ExtractionStore store, ExtractionQueue queue) {
        this.store = store;
        this.queue = queue;
    }

    @Override
    public void run(ApplicationArguments args) {
        List<UUID> unfinished = store.reclaimStalled(true);
        if (unfinished.isEmpty()) {
            return;
        }
        log.info("Resuming {} extraction(s) left unfinished by the previous run", unfinished.size());
        unfinished.forEach(queue::submit);
    }
}
