package com.claimguard.settings.dto;

public record CapabilityResponse(
        String key,
        String name,
        boolean configured,
        String detail) {
}
