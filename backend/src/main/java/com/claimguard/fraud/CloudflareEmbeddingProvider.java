package com.claimguard.fraud;

import com.claimguard.ai.AiRequestException;
import com.claimguard.ai.JsonHttpClient;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.node.ArrayNode;
import tools.jackson.databind.node.ObjectNode;

import java.util.Map;

public class CloudflareEmbeddingProvider implements EmbeddingProvider {

    private final JsonHttpClient http;
    private final String apiToken;
    private final String model;
    private final String path;
    private final int dimensions;

    public CloudflareEmbeddingProvider(JsonHttpClient http,
            String apiToken,
            String accountId,
            String model,
            int dimensions) {
        this.http = http;
        this.apiToken = apiToken;
        this.model = model;
        this.path = "/client/v4/accounts/" + accountId + "/ai/run/" + model;
        this.dimensions = dimensions;
    }

    @Override
    public boolean isAvailable() {
        return true;
    }

    @Override
    public String modelName() {
        return model;
    }

    @Override
    public int dimensions() {
        return dimensions;
    }

    @Override
    public float[] embed(String text) {
        ObjectNode body = http.mapper().createObjectNode();
        ArrayNode inputs = http.mapper().createArrayNode();
        inputs.add(text);
        body.set("text", inputs);

        JsonNode response = http.post(path, body, Map.of("Authorization", "Bearer " + apiToken));
        JsonNode vector = response.path("result").path("data").path(0);
        if (!vector.isArray() || vector.isEmpty()) {
            throw new AiRequestException("Embedding provider returned no vector");
        }

        float[] values = new float[vector.size()];
        for (int index = 0; index < vector.size(); index++) {
            values[index] = (float) vector.get(index).asDouble();
        }
        if (values.length != dimensions) {
            throw new AiRequestException(
                    "Embedding dimension mismatch: expected " + dimensions + " but got " + values.length);
        }
        return values;
    }
}
