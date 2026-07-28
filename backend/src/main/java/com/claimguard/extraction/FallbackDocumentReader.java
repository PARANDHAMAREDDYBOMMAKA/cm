package com.claimguard.extraction;

import com.claimguard.ai.AiRequestException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.stream.Collectors;

public class FallbackDocumentReader implements DocumentReader {

    private static final Logger log = LoggerFactory.getLogger(FallbackDocumentReader.class);

    private final List<DocumentReader> readers;

    public FallbackDocumentReader(List<DocumentReader> readers) {
        this.readers = List.copyOf(readers);
    }

    @Override
    public boolean isAvailable() {
        return readers.stream().anyMatch(DocumentReader::isAvailable);
    }

    @Override
    public String modelName() {
        return readers.stream().map(DocumentReader::modelName).collect(Collectors.joining(" → "));
    }

    @Override
    public ExtractedDocument read(byte[] content, String contentType, String filename) {
        RuntimeException last = null;
        for (DocumentReader reader : readers) {
            if (!reader.isAvailable()) {
                continue;
            }
            try {
                return reader.read(content, contentType, filename);
            } catch (RuntimeException exception) {
                log.warn("Reader {} failed, trying the next one: {}", reader.modelName(), exception.getMessage());
                last = exception;
            }
        }
        if (last != null) {
            throw new AiRequestException("All readers failed (" + modelName() + "). Last error: " + last.getMessage(), last);
        }
        throw new AiRequestException("No document reader is available");
    }
}
