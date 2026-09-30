package com.claimguard.upstash;

import com.claimguard.ai.JsonHttpClient;
import tools.jackson.databind.JsonNode;

import java.net.URI;
import java.util.LinkedHashMap;
import java.util.Map;

public class QStashClient {

    private final JsonHttpClient http;
    private final String baseUrl;
    private final String token;

    public QStashClient(JsonHttpClient http, String baseUrl, String token) {
        this.http = http;
        this.baseUrl = baseUrl.endsWith("/") ? baseUrl.substring(0, baseUrl.length() - 1) : baseUrl;
        this.token = token;
    }

    public String publish(String destination, Object body, Map<String, String> headers) {
        Map<String, String> outgoing = new LinkedHashMap<>(headers);
        outgoing.put("Authorization", "Bearer " + token);
        JsonNode response = http.post(URI.create(baseUrl + "/v2/publish/" + destination), body, outgoing);
        return response.path("messageId").asString();
    }
}
