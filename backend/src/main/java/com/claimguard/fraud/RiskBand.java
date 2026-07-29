package com.claimguard.fraud;

public enum RiskBand {
    CLEAN,
    LOW,
    MEDIUM,
    HIGH,
    CRITICAL;

    public static RiskBand of(int score) {
        if (score <= 0) {
            return CLEAN;
        }
        if (score < 25) {
            return LOW;
        }
        if (score < 50) {
            return MEDIUM;
        }
        if (score < 75) {
            return HIGH;
        }
        return CRITICAL;
    }

    public boolean needsReview() {
        return this == HIGH || this == CRITICAL;
    }
}
