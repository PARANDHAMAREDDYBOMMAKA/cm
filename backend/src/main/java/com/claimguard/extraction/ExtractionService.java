package com.claimguard.extraction;

import com.claimguard.storage.StorageService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.InputStream;
import java.util.Optional;
import java.util.UUID;

@Service
public class ExtractionService {

    private static final Logger log = LoggerFactory.getLogger(ExtractionService.class);

    private final ExtractionStore store;
    private final StorageService storage;
    private final DocumentReader reader;
    private final ClaimNormalizer normalizer;

    public ExtractionService(ExtractionStore store,
            StorageService storage,
            DocumentReader reader,
            ClaimNormalizer normalizer) {
        this.store = store;
        this.storage = storage;
        this.reader = reader;
        this.normalizer = normalizer;
    }

    public void process(UUID documentId) {
        if (!reader.isAvailable()) {
            store.fail(documentId, ExtractionStatus.SKIPPED, "No document reader is configured");
            return;
        }
        Optional<ExtractionStore.PendingJob> job = store.begin(documentId);
        if (job.isEmpty()) {
            return;
        }
        ExtractionStore.PendingJob pending = job.get();
        try {
            byte[] content = download(pending.storageKey());
            DocumentReader.ExtractedDocument raw = reader.read(content, pending.contentType(), pending.filename());
            store.complete(documentId, normalizer.normalize(raw));
        } catch (Exception exception) {
            log.warn("Extraction failed for document {}", documentId, exception);
            store.fail(documentId, ExtractionStatus.FAILED, message(exception));
        }
    }

    private byte[] download(String storageKey) throws IOException {
        StorageService.RetrievedObject object = storage.retrieve(storageKey);
        try (InputStream stream = object.content()) {
            return stream.readAllBytes();
        }
    }

    private static String message(Exception exception) {
        String message = exception.getMessage();
        return message != null ? message : exception.getClass().getSimpleName();
    }
}
