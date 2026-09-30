package com.claimguard.upstash;

import com.claimguard.ai.JsonHttpClient;
import tools.jackson.databind.JsonNode;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class UpstashRedis {

    private final JsonHttpClient http;
    private final String token;

    public UpstashRedis(JsonHttpClient http, String token) {
        this.http = http;
        this.token = token;
    }

    public JsonNode eval(String script, List<String> keys, List<String> args) {
        List<String> command = new ArrayList<>();
        command.add("EVAL");
        command.add(script);
        command.add(String.valueOf(keys.size()));
        command.addAll(keys);
        command.addAll(args);
        return execute(command);
    }

    private JsonNode execute(List<String> command) {
        JsonNode response = http.post("/", command, Map.of("Authorization", "Bearer " + token));
        if (response.hasNonNull("error")) {
            throw new IllegalStateException("Redis refused " + command.get(0) + ": " + response.get("error").asString());
        }
        return response.path("result");
    }
}
