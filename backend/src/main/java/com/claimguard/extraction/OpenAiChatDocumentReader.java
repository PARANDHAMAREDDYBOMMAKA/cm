package com.claimguard.extraction;

import com.claimguard.ai.AiRequestException;
import com.claimguard.ai.JsonHttpClient;
import com.claimguard.support.Retries;
import com.claimguard.support.Values;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;
import tools.jackson.databind.node.ArrayNode;
import tools.jackson.databind.node.ObjectNode;

import java.time.Duration;
import java.util.Base64;
import java.util.Map;

public class OpenAiChatDocumentReader implements DocumentReader {

    private static final long MAX_IMAGE_BYTES = 4L * 1024 * 1024;
    private static final int ATTEMPTS = 4;
    private static final Duration INITIAL_BACKOFF = Duration.ofSeconds(15);

    private final JsonHttpClient http;
    private final String apiKey;
    private final String model;
    private final String chatPath;
    private final int maxTokens;
    private final Map<String, String> extraOptions;

    public OpenAiChatDocumentReader(JsonHttpClient http,
            String apiKey,
            String model,
            String chatPath,
            int maxTokens,
            Map<String, String> extraOptions) {
        this.http = http;
        this.apiKey = apiKey;
        this.model = model;
        this.chatPath = chatPath;
        this.maxTokens = maxTokens;
        this.extraOptions = Map.copyOf(extraOptions);
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
    public ExtractedDocument read(byte[] content, String contentType, String filename) {
        if (!MimeTypes.isImage(contentType)) {
            throw new AiRequestException("This reader only accepts images, not " + MimeTypes.normalize(contentType));
        }
        if (content.length > MAX_IMAGE_BYTES) {
            throw new AiRequestException("Image is too large (" + content.length + " bytes)");
        }
        JsonNode response = Retries.withBackoff(
                ATTEMPTS,
                INITIAL_BACKOFF,
                exception -> exception instanceof AiRequestException failure && failure.isRetryable(),
                () -> http.post(chatPath, requestBody(content, contentType),
                        Map.of("Authorization", "Bearer " + apiKey)));
        return ExtractionParser.parse(http.mapper(), model, extractText(response));
    }

    private ObjectNode requestBody(byte[] content, String contentType) {
        JsonMapper mapper = http.mapper();

        ObjectNode imageUrl = mapper.createObjectNode();
        imageUrl.put("url", "data:" + MimeTypes.normalize(contentType) + ";base64,"
                + Base64.getEncoder().encodeToString(content));

        ArrayNode parts = mapper.createArrayNode();
        parts.add(mapper.createObjectNode().put("type", "text").put("text", ExtractionPrompt.TEXT));
        parts.add(mapper.createObjectNode().put("type", "image_url").set("image_url", imageUrl));

        ArrayNode messages = mapper.createArrayNode();
        messages.add(mapper.createObjectNode().put("role", "user").set("content", parts));

        ObjectNode body = mapper.createObjectNode();
        body.put("model", model);
        body.put("temperature", 0);
        body.put("max_completion_tokens", maxTokens);
        extraOptions.forEach(body::put);
        body.set("messages", messages);
        return body;
    }

    private static String extractText(JsonNode response) {
        JsonNode error = response.path("error");
        if (!error.isMissingNode() && !error.isNull()) {
            throw new AiRequestException("Provider rejected the request: " + error.path("message").asString());
        }
        JsonNode content = response.path("choices").path(0).path("message").path("content");
        if (content.isObject() || content.isArray()) {
            return content.toString();
        }
        String value = Values.text(content.asString(""));
        if (value == null) {
            throw new AiRequestException("Provider returned no readable content");
        }
        return value;
    }
}
