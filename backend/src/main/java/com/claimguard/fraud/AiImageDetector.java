package com.claimguard.fraud;

import java.math.BigDecimal;

public interface AiImageDetector {

    boolean isAvailable();

    String name();

    BigDecimal generatedProbability(byte[] image, String contentType);
}
