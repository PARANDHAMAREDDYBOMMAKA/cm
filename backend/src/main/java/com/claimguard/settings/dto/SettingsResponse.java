package com.claimguard.settings.dto;

import java.util.List;

public record SettingsResponse(
        List<CapabilityResponse> capabilities,
        List<SettingResponse> thresholds,
        boolean auditIntact,
        long auditEvents,
        String auditMessage) {
}
