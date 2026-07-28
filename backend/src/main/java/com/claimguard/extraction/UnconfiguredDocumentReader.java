package com.claimguard.extraction;

public class UnconfiguredDocumentReader implements DocumentReader {

    @Override
    public boolean isAvailable() {
        return false;
    }

    @Override
    public String modelName() {
        return "none";
    }

    @Override
    public ExtractedDocument read(byte[] content, String contentType, String filename) {
        throw new IllegalStateException("No document reader is configured");
    }
}
