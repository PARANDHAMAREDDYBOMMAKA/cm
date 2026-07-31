package com.claimguard.analytics;

import com.claimguard.ai.JsonHttpClient;

import java.util.LinkedHashMap;
import java.util.Map;

public class PostHogAnalytics implements Analytics {

    private final JsonHttpClient http;
    private final String apiKey;

    public PostHogAnalytics(JsonHttpClient http, String apiKey) {
        this.http = http;
        this.apiKey = apiKey;
    }

    @Override
    public boolean isAvailable() {
        return true;
    }

    @Override
    public void capture(String event, String distinctId, Map<String, Object> properties) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("api_key", apiKey);
        payload.put("event", event);
        payload.put("distinct_id", distinctId);
        payload.put("properties", properties);
        http.post("/i/v0/e/", payload, Map.of());
    }
}
