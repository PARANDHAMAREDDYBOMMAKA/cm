package com.claimguard.support;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.Locale;

public final class Values {

    private static final List<DateTimeFormatter> DATE_FORMATS = List.of(
            DateTimeFormatter.ISO_LOCAL_DATE,
            DateTimeFormatter.ofPattern("dd/MM/uuuu", Locale.ENGLISH),
            DateTimeFormatter.ofPattern("dd-MM-uuuu", Locale.ENGLISH),
            DateTimeFormatter.ofPattern("dd.MM.uuuu", Locale.ENGLISH),
            DateTimeFormatter.ofPattern("uuuu/MM/dd", Locale.ENGLISH),
            DateTimeFormatter.ofPattern("d MMM uuuu", Locale.ENGLISH),
            DateTimeFormatter.ofPattern("d MMMM uuuu", Locale.ENGLISH),
            DateTimeFormatter.ofPattern("MMM d, uuuu", Locale.ENGLISH),
            DateTimeFormatter.ofPattern("dd/MM/uu", Locale.ENGLISH));

    private static final String AMOUNT_NOISE = "[^0-9.\\-]";

    private Values() {
    }

    public static String text(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        if (trimmed.isEmpty() || trimmed.equalsIgnoreCase("null") || trimmed.equalsIgnoreCase("n/a")) {
            return null;
        }
        return trimmed;
    }

    public static String truncate(String value, int max) {
        String trimmed = text(value);
        if (trimmed == null) {
            return null;
        }
        return trimmed.length() <= max ? trimmed : trimmed.substring(0, max);
    }

    public static BigDecimal decimal(String value) {
        String trimmed = text(value);
        if (trimmed == null) {
            return null;
        }
        String cleaned = trimmed.replaceAll(AMOUNT_NOISE, "");
        if (cleaned.isEmpty() || cleaned.equals("-") || cleaned.equals(".")) {
            return null;
        }
        try {
            return new BigDecimal(cleaned);
        } catch (NumberFormatException exception) {
            return null;
        }
    }

    public static LocalDate date(String value) {
        String trimmed = text(value);
        if (trimmed == null) {
            return null;
        }
        for (DateTimeFormatter format : DATE_FORMATS) {
            try {
                return LocalDate.parse(trimmed, format);
            } catch (DateTimeParseException ignored) {
                continue;
            }
        }
        return null;
    }

    public static double clampConfidence(double value) {
        if (Double.isNaN(value) || value < 0) {
            return 0;
        }
        return Math.min(value, 1);
    }
}
