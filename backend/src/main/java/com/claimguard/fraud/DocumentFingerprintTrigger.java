package com.claimguard.fraud;

import com.claimguard.extraction.DocumentUploadedEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.task.TaskExecutor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
public class DocumentFingerprintTrigger {

    private static final Logger log = LoggerFactory.getLogger(DocumentFingerprintTrigger.class);

    private final TaskExecutor executor;
    private final DocumentIntake intake;

    public DocumentFingerprintTrigger(TaskExecutor extractionTaskExecutor, DocumentIntake intake) {
        this.executor = extractionTaskExecutor;
        this.intake = intake;
    }

    @TransactionalEventListener
    public void onDocumentUploaded(DocumentUploadedEvent event) {
        executor.execute(() -> {
            try {
                intake.fingerprint(event.documentId());
            } catch (RuntimeException exception) {
                log.warn("Fingerprinting failed for document {}: {}",
                        event.documentId(), exception.getMessage());
            }
        });
    }
}
