package com.claimguard.fraud;

public enum Severity {
    LOW(10),
    MEDIUM(25),
    HIGH(45),
    CRITICAL(70);

    private final int weight;

    Severity(int weight) {
        this.weight = weight;
    }

    public int weight() {
        return weight;
    }
}
