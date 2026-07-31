package com.claimguard.settings.dto;

public record SettingResponse(
        String name,
        String value,
        String description) {
}
