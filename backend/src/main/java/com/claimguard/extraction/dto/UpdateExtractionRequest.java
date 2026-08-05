package com.claimguard.extraction.dto;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;

import java.util.Map;

public record UpdateExtractionRequest(
        @NotEmpty(message = "at least one field is required")
        @Size(max = 32, message = "at most 32 fields may be corrected at once")
        Map<String, @Size(max = 2000, message = "field values must be at most 2000 characters") String> fields) {
}
