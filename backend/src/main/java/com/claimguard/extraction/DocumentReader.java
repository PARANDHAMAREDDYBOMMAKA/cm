package com.claimguard.extraction;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

public interface DocumentReader {

    boolean isAvailable();

    String modelName();

    ExtractedDocument read(byte[] content, String contentType, String filename);

    record ExtractedDocument(
            String model,
            String documentType,
            String patientName,
            String patientAge,
            String patientGender,
            String patientId,
            String providerName,
            String providerAddress,
            String diagnosis,
            List<String> procedures,
            String admissionDate,
            String dischargeDate,
            String invoiceNumber,
            String invoiceDate,
            BigDecimal totalAmount,
            String currency,
            List<LineItem> lineItems,
            Map<String, Double> confidence,
            String raw) {
    }

    record LineItem(
            String description,
            String code,
            BigDecimal quantity,
            BigDecimal unitAmount,
            BigDecimal amount) {
    }
}
