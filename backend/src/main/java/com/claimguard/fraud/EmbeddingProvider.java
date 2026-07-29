package com.claimguard.fraud;

public interface EmbeddingProvider {

    boolean isAvailable();

    String modelName();

    int dimensions();

    float[] embed(String text);
}
