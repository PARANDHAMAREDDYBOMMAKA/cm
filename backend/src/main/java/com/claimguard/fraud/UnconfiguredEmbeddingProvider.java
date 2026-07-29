package com.claimguard.fraud;

public class UnconfiguredEmbeddingProvider implements EmbeddingProvider {

    @Override
    public boolean isAvailable() {
        return false;
    }

    @Override
    public String modelName() {
        return "none";
    }

    @Override
    public int dimensions() {
        return 0;
    }

    @Override
    public float[] embed(String text) {
        throw new IllegalStateException("No embedding provider is configured");
    }
}
