package com.claimguard.extraction;

import com.claimguard.extraction.dto.ExtractionResponse;
import com.claimguard.extraction.dto.LineItemResponse;

import java.util.List;
import java.util.Map;

public final class ExtractionMapper {

    private ExtractionMapper() {
    }

    public static ExtractionResponse toResponse(DocumentExtraction extraction) {
        if (extraction == null) {
            return null;
        }
        return new ExtractionResponse(
                extraction.getId(),
                extraction.getStatus().name(),
                extraction.getModel(),
                extraction.getDocumentType(),
                extraction.getPatientName(),
                extraction.getPatientAge(),
                extraction.getPatientGender(),
                extraction.getPatientId(),
                extraction.getProviderName(),
                extraction.getProviderAddress(),
                extraction.getDiagnosis(),
                extraction.getProcedures() != null ? extraction.getProcedures() : List.of(),
                extraction.getAdmissionDate(),
                extraction.getDischargeDate(),
                extraction.getInvoiceNumber(),
                extraction.getInvoiceDate(),
                extraction.getTotalAmount(),
                extraction.getCurrency(),
                extraction.getLineItems().stream().map(ExtractionMapper::toResponse).toList(),
                extraction.getConfidence() != null ? extraction.getConfidence() : Map.of(),
                extraction.getEditedFields() != null ? extraction.getEditedFields() : List.of(),
                extraction.getError(),
                extraction.getUpdatedAt());
    }

    private static LineItemResponse toResponse(ExtractionLineItem item) {
        return new LineItemResponse(
                item.getId(),
                item.getDescription(),
                item.getCode(),
                item.getQuantity(),
                item.getUnitAmount(),
                item.getAmount());
    }
}
