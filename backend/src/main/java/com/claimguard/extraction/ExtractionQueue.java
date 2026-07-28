package com.claimguard.extraction;

import java.util.UUID;

public interface ExtractionQueue {

    void submit(UUID documentId);
}
