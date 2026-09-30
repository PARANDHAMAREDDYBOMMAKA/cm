package com.claimguard.upstash;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.time.Clock;
import java.util.Base64;
import java.util.List;
import java.util.Objects;

public class QStashVerifier {

    private static final long CLOCK_SKEW_SECONDS = 60;

    private final List<String> signingKeys;
    private final String destination;
    private final JsonMapper mapper;
    private final Clock clock;

    public QStashVerifier(List<String> signingKeys, String destination, JsonMapper mapper, Clock clock) {
        this.signingKeys = signingKeys.stream().filter(Objects::nonNull).filter(key -> !key.isBlank()).toList();
        this.destination = destination;
        this.mapper = mapper;
        this.clock = clock;
    }

    public boolean isConfigured() {
        return !signingKeys.isEmpty() && destination != null && !destination.isBlank();
    }

    public String destination() {
        return destination;
    }

    public boolean verify(String signature, byte[] body) {
        if (!isConfigured() || signature == null || signature.isBlank()) {
            return false;
        }
        String[] parts = signature.split("\\.");
        if (parts.length != 3) {
            return false;
        }
        try {
            byte[] presented = Base64.getUrlDecoder().decode(parts[2]);
            String signed = parts[0] + "." + parts[1];
            boolean trusted = signingKeys.stream()
                    .anyMatch(key -> MessageDigest.isEqual(hmac(key, signed), presented));
            return trusted && claimsMatch(mapper.readTree(Base64.getUrlDecoder().decode(parts[1])), body);
        } catch (IllegalArgumentException | tools.jackson.core.JacksonException exception) {
            return false;
        }
    }

    private boolean claimsMatch(JsonNode claims, byte[] body) {
        long now = clock.instant().getEpochSecond();
        return "Upstash".equals(claims.path("iss").asString())
                && destination.equals(claims.path("sub").asString())
                && claims.path("exp").asLong() + CLOCK_SKEW_SECONDS >= now
                && claims.path("nbf").asLong() - CLOCK_SKEW_SECONDS <= now
                && unpadded(claims.path("body").asString()).equals(unpadded(bodyHash(body)));
    }

    private static byte[] hmac(String key, String data) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(key.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            return mac.doFinal(data.getBytes(StandardCharsets.UTF_8));
        } catch (GeneralSecurityException exception) {
            throw new IllegalStateException("HmacSHA256 is unavailable", exception);
        }
    }

    private static String bodyHash(byte[] body) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256").digest(body == null ? new byte[0] : body);
            return Base64.getUrlEncoder().encodeToString(digest);
        } catch (GeneralSecurityException exception) {
            throw new IllegalStateException("SHA-256 is unavailable", exception);
        }
    }

    private static String unpadded(String value) {
        return value == null ? "" : value.replaceAll("=+$", "");
    }
}
