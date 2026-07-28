package com.claimguard.ai;

import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

import java.time.Duration;
import java.util.Map;

public class JsonHttpClient {

    private final RestClient client;
    private final JsonMapper mapper;

    public JsonHttpClient(String baseUrl, Duration timeout, JsonMapper mapper) {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(Duration.ofSeconds(10));
        factory.setReadTimeout(timeout);
        this.client = RestClient.builder().baseUrl(baseUrl).requestFactory(factory).build();
        this.mapper = mapper;
    }

    public JsonNode post(String path, Object body, Map<String, String> headers) {
        Result result;
        try {
            result = client.post()
                    .uri(path)
                    .contentType(MediaType.APPLICATION_JSON)
                    .headers(target -> headers.forEach(target::set))
                    .body(mapper.writeValueAsString(body))
                    .exchange((request, response) -> new Result(
                            response.getStatusCode().value(),
                            response.bodyTo(String.class)));
        } catch (RestClientException exception) {
            throw new AiRequestException(exception.getMessage(), exception);
        }

        if (result.status() >= 400) {
            throw new AiRequestException("HTTP " + result.status() + ": " + summarize(result.body()), result.status());
        }
        return mapper.readTree(result.body() == null ? "{}" : result.body());
    }

    public JsonMapper mapper() {
        return mapper;
    }

    private static String summarize(String body) {
        if (body == null || body.isBlank()) {
            return "no response body";
        }
        String collapsed = body.replaceAll("\\s+", " ").trim();
        return collapsed.length() <= 500 ? collapsed : collapsed.substring(0, 500) + "…";
    }

    private record Result(int status, String body) {
    }
}
