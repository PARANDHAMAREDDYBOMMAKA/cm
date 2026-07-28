package com.claimguard.extraction;

import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
public class ExtractionTrigger {

    private final ExtractionLauncher launcher;

    public ExtractionTrigger(ExtractionLauncher launcher) {
        this.launcher = launcher;
    }

    @TransactionalEventListener
    public void onDocumentUploaded(DocumentUploadedEvent event) {
        launcher.launch(event.documentId());
    }
}
