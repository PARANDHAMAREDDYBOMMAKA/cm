package com.claimguard.extraction;

import org.springframework.core.task.TaskExecutor;

import java.util.UUID;

public class LocalExtractionQueue implements ExtractionQueue {

    private final TaskExecutor executor;
    private final ExtractionService service;

    public LocalExtractionQueue(TaskExecutor executor, ExtractionService service) {
        this.executor = executor;
        this.service = service;
    }

    @Override
    public void submit(UUID documentId) {
        executor.execute(() -> service.process(documentId));
    }
}
