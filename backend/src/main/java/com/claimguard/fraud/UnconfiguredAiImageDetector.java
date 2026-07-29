package com.claimguard.fraud;

import java.math.BigDecimal;

public class UnconfiguredAiImageDetector implements AiImageDetector {

    @Override
    public boolean isAvailable() {
        return false;
    }

    @Override
    public String name() {
        return "none";
    }

    @Override
    public BigDecimal generatedProbability(byte[] image, String contentType) {
        throw new IllegalStateException("No AI image detector is configured");
    }
}
