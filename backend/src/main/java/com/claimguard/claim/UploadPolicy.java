package com.claimguard.claim;

import java.util.Set;

public final class UploadPolicy {

    public static final String PDF = "application/pdf";
    public static final String PNG = "image/png";
    public static final String JPEG = "image/jpeg";
    public static final String WEBP = "image/webp";
    public static final String TIFF = "image/tiff";

    private static final Set<String> ACCEPTED = Set.of(PDF, PNG, JPEG, WEBP, TIFF);
    private static final Set<String> INLINE_SAFE = Set.of(PDF, PNG, JPEG, WEBP);

    private UploadPolicy() {
    }

    public static Set<String> acceptedTypes() {
        return ACCEPTED;
    }

    public static boolean isAccepted(String contentType) {
        return contentType != null && ACCEPTED.contains(contentType);
    }

    public static boolean isInlineSafe(String contentType) {
        return contentType != null && INLINE_SAFE.contains(contentType);
    }

    public static String sniff(byte[] content) {
        if (content == null) {
            return null;
        }
        if (startsWith(content, 0x25, 0x50, 0x44, 0x46)) {
            return PDF;
        }
        if (startsWith(content, 0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A)) {
            return PNG;
        }
        if (startsWith(content, 0xFF, 0xD8, 0xFF)) {
            return JPEG;
        }
        if (startsWith(content, 0x52, 0x49, 0x46, 0x46) && matchesAt(content, 8, 0x57, 0x45, 0x42, 0x50)) {
            return WEBP;
        }
        if (startsWith(content, 0x49, 0x49, 0x2A, 0x00) || startsWith(content, 0x4D, 0x4D, 0x00, 0x2A)) {
            return TIFF;
        }
        return null;
    }

    private static boolean startsWith(byte[] content, int... signature) {
        return matchesAt(content, 0, signature);
    }

    private static boolean matchesAt(byte[] content, int offset, int... signature) {
        if (content.length < offset + signature.length) {
            return false;
        }
        for (int index = 0; index < signature.length; index++) {
            if ((content[offset + index] & 0xFF) != signature[index]) {
                return false;
            }
        }
        return true;
    }
}
