package com.claimguard.upstash;

import org.junit.jupiter.api.Test;
import tools.jackson.databind.json.JsonMapper;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Arrays;
import java.util.Base64;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class QStashVerifierTest {

    private static final String DESTINATION = "https://api.example.com/api/queue/extraction";
    private static final Instant NOW = Instant.parse("2026-09-30T12:00:00Z");
    private static final byte[] BODY = "{\"documentId\":\"00000000-0000-0000-0000-000000000001\"}"
            .getBytes(StandardCharsets.UTF_8);

    private final QStashVerifier verifier = new QStashVerifier(
            List.of("current-key", "next-key"),
            DESTINATION,
            JsonMapper.builder().build(),
            Clock.fixed(NOW, ZoneOffset.UTC));

    @Test
    void acceptsATokenSignedWithEitherKey() throws Exception {
        assertThat(verifier.verify(sign("current-key", DESTINATION, BODY, NOW), BODY)).isTrue();
        assertThat(verifier.verify(sign("next-key", DESTINATION, BODY, NOW), BODY)).isTrue();
    }

    @Test
    void rejectsAnUnknownKey() throws Exception {
        assertThat(verifier.verify(sign("someone-else", DESTINATION, BODY, NOW), BODY)).isFalse();
    }

    @Test
    void rejectsABodyThatWasSwappedAfterSigning() throws Exception {
        String signature = sign("current-key", DESTINATION, BODY, NOW);
        byte[] swapped = "{\"documentId\":\"00000000-0000-0000-0000-000000000002\"}".getBytes(StandardCharsets.UTF_8);

        assertThat(verifier.verify(signature, swapped)).isFalse();
    }

    @Test
    void rejectsATokenMeantForAnotherDestination() throws Exception {
        assertThat(verifier.verify(sign("current-key", "https://evil.example.com/hook", BODY, NOW), BODY)).isFalse();
    }

    @Test
    void rejectsAnExpiredToken() throws Exception {
        assertThat(verifier.verify(sign("current-key", DESTINATION, BODY, NOW.minusSeconds(3600)), BODY)).isFalse();
    }

    @Test
    void rejectsMissingOrMalformedSignatures() {
        assertThat(verifier.verify(null, BODY)).isFalse();
        assertThat(verifier.verify("not-a-jwt", BODY)).isFalse();
        assertThat(verifier.verify("a.b.c", BODY)).isFalse();
    }

    @Test
    void refusesEverythingWhenNoKeysAreConfigured() throws Exception {
        QStashVerifier unconfigured = new QStashVerifier(Arrays.asList(null, " "), DESTINATION,
                JsonMapper.builder().build(), Clock.fixed(NOW, ZoneOffset.UTC));

        assertThat(unconfigured.isConfigured()).isFalse();
        assertThat(unconfigured.verify(sign("current-key", DESTINATION, BODY, NOW), BODY)).isFalse();
    }

    private static String sign(String key, String subject, byte[] body, Instant issuedAt) throws Exception {
        Base64.Encoder encoder = Base64.getUrlEncoder().withoutPadding();
        String bodyHash = Base64.getUrlEncoder().encodeToString(MessageDigest.getInstance("SHA-256").digest(body));
        String header = encoder.encodeToString("{\"alg\":\"HS256\",\"typ\":\"JWT\"}".getBytes(StandardCharsets.UTF_8));
        String claims = encoder.encodeToString(("{\"iss\":\"Upstash\",\"sub\":\"" + subject + "\","
                + "\"exp\":" + issuedAt.plusSeconds(300).getEpochSecond() + ","
                + "\"nbf\":" + issuedAt.getEpochSecond() + ","
                + "\"iat\":" + issuedAt.getEpochSecond() + ","
                + "\"jti\":\"msg_1\",\"body\":\"" + bodyHash + "\"}").getBytes(StandardCharsets.UTF_8));
        Mac mac = Mac.getInstance("HmacSHA256");
        mac.init(new SecretKeySpec(key.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
        String signature = encoder.encodeToString(mac.doFinal((header + "." + claims).getBytes(StandardCharsets.UTF_8)));
        return header + "." + claims + "." + signature;
    }
}
