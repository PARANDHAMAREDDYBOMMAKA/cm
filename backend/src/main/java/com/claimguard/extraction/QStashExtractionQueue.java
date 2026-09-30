package com.claimguard.extraction;

import com.claimguard.extraction.dto.ExtractionMessage;
import com.claimguard.upstash.QStashClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.UUID;

public class QStashExtractionQueue implements ExtractionQueue {

    private static final Logger log = LoggerFactory.getLogger(QStashExtractionQueue.class);

    private final QStashClient qstash;
    private final String destination;
    private final Map<String, String> headers;

    public QStashExtractionQueue(QStashClient qstash, String destination, int parallelism, int retries) {
        this.qstash = qstash;
        this.destination = destination;
        this.headers = Map.of(
                "Upstash-Retries", String.valueOf(retries),
                "Upstash-Flow-Control-Key", "claimguard-extraction",
                "Upstash-Flow-Control-Value", "parallelism=" + parallelism);
    }

    @Override
    public void submit(UUID documentId) {
        try {
            String messageId = qstash.publish(destination, new ExtractionMessage(documentId), headers);
            log.debug("Queued extraction for document {} as {}", documentId, messageId);
        } catch (RuntimeException exception) {
            log.warn("Could not queue extraction for document {}, the sweeper will pick it up: {}",
                    documentId, exception.getMessage());
        }
    }
}
