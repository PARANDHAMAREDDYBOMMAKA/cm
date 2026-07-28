package com.claimguard.extraction;

import com.claimguard.support.Values;

import java.util.Locale;

public final class MimeTypes {

    private static final String FALLBACK = "application/octet-stream";

    private MimeTypes() {
    }

    public static String normalize(String contentType) {
        String value = Values.text(contentType);
        if (value == null) {
            return FALLBACK;
        }
        int separator = value.indexOf(';');
        String base = separator > 0 ? value.substring(0, separator) : value;
        return base.trim().toLowerCase(Locale.ENGLISH);
    }

    public static boolean isImage(String contentType) {
        return normalize(contentType).startsWith("image/");
    }
}
