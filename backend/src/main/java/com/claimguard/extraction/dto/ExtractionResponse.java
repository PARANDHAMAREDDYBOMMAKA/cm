package com.claimguard.extraction.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public record ExtractionResponse(
        UUID id,
        String status,
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
        LocalDate admissionDate,
        LocalDate dischargeDate,
        String invoiceNumber,
        LocalDate invoiceDate,
        BigDecimal totalAmount,
        String currency,
        List<LineItemResponse> lineItems,
        Map<String, Double> confidence,
        List<String> editedFields,
        String error,
        Instant updatedAt) {
}
