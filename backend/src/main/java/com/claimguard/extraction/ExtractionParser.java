package com.claimguard.extraction;

import com.claimguard.ai.AiRequestException;
import com.claimguard.support.Values;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class ExtractionParser {

    private ExtractionParser() {
    }

    public static DocumentReader.ExtractedDocument parse(JsonMapper mapper, String model, String text) {
        String payload = isolateJson(stripFence(stripReasoning(text)));
        JsonNode root;
        try {
            root = mapper.readTree(payload);
        } catch (RuntimeException exception) {
            throw new AiRequestException("Model did not return valid JSON: " + exception.getMessage(), exception);
        }
        return new DocumentReader.ExtractedDocument(
                model,
                string(root, ExtractionField.DOCUMENT_TYPE),
                string(root, ExtractionField.PATIENT_NAME),
                string(root, ExtractionField.PATIENT_AGE),
                string(root, ExtractionField.PATIENT_GENDER),
                string(root, ExtractionField.PATIENT_ID),
                string(root, ExtractionField.PROVIDER_NAME),
                string(root, ExtractionField.PROVIDER_ADDRESS),
                string(root, ExtractionField.DIAGNOSIS),
                strings(root.path(ExtractionField.PROCEDURES)),
                string(root, ExtractionField.ADMISSION_DATE),
                string(root, ExtractionField.DISCHARGE_DATE),
                string(root, ExtractionField.INVOICE_NUMBER),
                string(root, ExtractionField.INVOICE_DATE),
                decimal(root, ExtractionField.TOTAL_AMOUNT),
                string(root, ExtractionField.CURRENCY),
                lineItems(root.path(ExtractionField.LINE_ITEMS)),
                confidence(root.path("confidence")),
                payload);
    }

    private static String stripReasoning(String text) {
        int close = text.lastIndexOf("</think>");
        return close < 0 ? text : text.substring(close + "</think>".length());
    }

    private static String isolateJson(String text) {
        int start = text.indexOf('{');
        int end = text.lastIndexOf('}');
        return start >= 0 && end > start ? text.substring(start, end + 1) : text;
    }

    private static String stripFence(String text) {
        String value = text.trim();
        if (!value.startsWith("```")) {
            return value;
        }
        int firstBreak = value.indexOf('\n');
        int lastFence = value.lastIndexOf("```");
        if (firstBreak < 0 || lastFence <= firstBreak) {
            return value;
        }
        return value.substring(firstBreak + 1, lastFence).trim();
    }

    private static String string(JsonNode node, String field) {
        JsonNode value = node.path(field);
        return value.isMissingNode() || value.isNull() ? null : Values.text(value.asString(""));
    }

    private static BigDecimal decimal(JsonNode node, String field) {
        JsonNode value = node.path(field);
        if (value.isMissingNode() || value.isNull()) {
            return null;
        }
        return value.isNumber() ? value.decimalValue() : Values.decimal(value.asString(""));
    }

    private static List<String> strings(JsonNode array) {
        List<String> values = new ArrayList<>();
        if (!array.isArray()) {
            return values;
        }
        for (JsonNode item : array) {
            String value = Values.text(item.asString(""));
            if (value != null) {
                values.add(value);
            }
        }
        return values;
    }

    private static List<DocumentReader.LineItem> lineItems(JsonNode array) {
        List<DocumentReader.LineItem> items = new ArrayList<>();
        if (!array.isArray()) {
            return items;
        }
        for (JsonNode item : array) {
            String description = string(item, "description");
            BigDecimal amount = decimal(item, "amount");
            if (description == null && amount == null) {
                continue;
            }
            items.add(new DocumentReader.LineItem(
                    description,
                    string(item, "code"),
                    decimal(item, "quantity"),
                    decimal(item, "unitAmount"),
                    amount));
        }
        return items;
    }

    private static Map<String, Double> confidence(JsonNode node) {
        Map<String, Double> scores = new LinkedHashMap<>();
        if (!node.isObject()) {
            return scores;
        }
        for (String field : ExtractionField.ALL) {
            JsonNode value = node.path(field);
            if (value.isNumber()) {
                scores.put(field, Values.clampConfidence(value.doubleValue()));
            }
        }
        return scores;
    }
}
