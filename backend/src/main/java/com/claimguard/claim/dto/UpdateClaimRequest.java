package com.claimguard.claim.dto;

import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record UpdateClaimRequest(
        @Size(max = 64, message = "reference must be at most 64 characters")
        @Pattern(regexp = "^$|^[\\p{Alnum}][\\p{Alnum} ._/-]*$",
                message = "reference may only contain letters, digits, spaces and . _ / -")
        String reference,

        @Size(max = 255, message = "claimantName must be at most 255 characters")
        String claimantName,

        @Size(max = 2000, message = "note must be at most 2000 characters")
        String note,

        @Size(max = 32, message = "status must be at most 32 characters")
        String status) {
}
