package com.claimguard.decision.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ReviewRequest(
        @NotBlank(message = "action is required")
        @Size(max = 32, message = "action must be at most 32 characters")
        String action,

        @Size(max = 2000, message = "note must be at most 2000 characters")
        String note) {
}
