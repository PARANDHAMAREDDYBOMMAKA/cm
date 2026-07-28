package com.claimguard.extraction.dto;

import java.math.BigDecimal;
import java.util.UUID;

public record LineItemResponse(
        UUID id,
        String description,
        String code,
        BigDecimal quantity,
        BigDecimal unitAmount,
        BigDecimal amount) {
}
