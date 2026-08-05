package com.claimguard.access;

import java.util.Locale;

public enum ClaimScope {
    NONE,
    USER,
    ORG;

    public static ClaimScope parse(String value) {
        if (value == null || value.isBlank()) {
            return ORG;
        }
        try {
            return valueOf(value.trim().toUpperCase(Locale.ENGLISH));
        } catch (IllegalArgumentException exception) {
            return ORG;
        }
    }
}
