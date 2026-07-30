package com.claimguard.audit;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.Map;
import java.util.TreeMap;

public final class AuditSeal {

    public static final String GENESIS_HASH = "0".repeat(64);

    private AuditSeal() {
    }

    public static String seal(AuditEvent event) {
        StringBuilder canonical = new StringBuilder()
                .append(event.getSeq()).append('|')
                .append(event.getClaimId()).append('|')
                .append(event.getActor()).append('|')
                .append(event.getAction()).append('|')
                .append(event.getSummary()).append('|')
                .append(flatten(event.getDetails())).append('|')
                .append(event.getCreatedAt()).append('|')
                .append(event.getPreviousHash());
        return sha256(canonical.toString());
    }

    private static String flatten(Map<String, String> details) {
        if (details == null || details.isEmpty()) {
            return "";
        }
        StringBuilder text = new StringBuilder();
        new TreeMap<>(details).forEach((key, value) -> text.append(key).append('=').append(value).append(';'));
        return text.toString();
    }

    private static String sha256(String value) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(digest.digest(value.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 is unavailable", exception);
        }
    }
}
