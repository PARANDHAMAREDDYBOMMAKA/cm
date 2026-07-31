package com.claimguard.analytics;

import java.util.Map;

public class UnconfiguredAnalytics implements Analytics {

    @Override
    public boolean isAvailable() {
        return false;
    }

    @Override
    public void capture(String event, String distinctId, Map<String, Object> properties) {
    }
}
