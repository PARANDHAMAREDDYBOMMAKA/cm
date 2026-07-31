package com.claimguard.analytics;

import java.util.Map;

public interface Analytics {

    boolean isAvailable();

    void capture(String event, String distinctId, Map<String, Object> properties);
}
