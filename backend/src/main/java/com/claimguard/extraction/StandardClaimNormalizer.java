package com.claimguard.extraction;

import com.claimguard.support.Values;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;

@Component
public class StandardClaimNormalizer implements ClaimNormalizer {

    @Override
    public DocumentReader.ExtractedDocument normalize(DocumentReader.ExtractedDocument document) {
        List<DocumentReader.LineItem> lineItems = normalizeLineItems(document.lineItems());
        return new DocumentReader.ExtractedDocument(
                document.model(),
                upper(document.documentType()),
                document.patientName(),
                document.patientAge(),
                gender(document.patientGender()),
                document.patientId(),
                document.providerName(),
                document.providerAddress(),
                document.diagnosis(),
                distinct(document.procedures()),
                document.admissionDate(),
                document.dischargeDate(),
                document.invoiceNumber(),
                document.invoiceDate(),
                totalAmount(document.totalAmount(), lineItems),
                currency(document.currency()),
                lineItems,
                document.confidence(),
                document.raw());
    }

    private static List<DocumentReader.LineItem> normalizeLineItems(List<DocumentReader.LineItem> items) {
        List<DocumentReader.LineItem> normalized = new ArrayList<>();
        if (items == null) {
            return normalized;
        }
        for (DocumentReader.LineItem item : items) {
            BigDecimal amount = item.amount();
            if (amount == null && item.quantity() != null && item.unitAmount() != null) {
                amount = item.quantity().multiply(item.unitAmount());
            }
            normalized.add(new DocumentReader.LineItem(
                    Values.truncate(item.description(), 512),
                    Values.truncate(item.code(), 64),
                    item.quantity(),
                    item.unitAmount(),
                    amount));
        }
        return normalized;
    }

    private static BigDecimal totalAmount(BigDecimal declared, List<DocumentReader.LineItem> items) {
        if (declared != null) {
            return declared;
        }
        BigDecimal sum = BigDecimal.ZERO;
        boolean any = false;
        for (DocumentReader.LineItem item : items) {
            if (item.amount() != null) {
                sum = sum.add(item.amount());
                any = true;
            }
        }
        return any ? sum : null;
    }

    private static List<String> distinct(List<String> values) {
        if (values == null) {
            return List.of();
        }
        LinkedHashSet<String> unique = new LinkedHashSet<>();
        for (String value : values) {
            String trimmed = Values.text(value);
            if (trimmed != null) {
                unique.add(trimmed);
            }
        }
        return List.copyOf(unique);
    }

    private static String currency(String value) {
        String trimmed = Values.text(value);
        if (trimmed == null) {
            return null;
        }
        String upper = trimmed.toUpperCase(Locale.ENGLISH);
        return switch (upper) {
            case "RS", "RS.", "INR", "₹" -> "INR";
            default -> upper.length() <= 8 ? upper : upper.substring(0, 8);
        };
    }

    private static String gender(String value) {
        String trimmed = Values.text(value);
        if (trimmed == null) {
            return null;
        }
        return switch (trimmed.toUpperCase(Locale.ENGLISH)) {
            case "M", "MALE" -> "MALE";
            case "F", "FEMALE" -> "FEMALE";
            default -> trimmed;
        };
    }

    private static String upper(String value) {
        String trimmed = Values.text(value);
        return trimmed == null ? null : trimmed.toUpperCase(Locale.ENGLISH);
    }
}
