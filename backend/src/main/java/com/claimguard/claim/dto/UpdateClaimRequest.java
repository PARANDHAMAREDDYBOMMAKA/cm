package com.claimguard.claim.dto;

public record UpdateClaimRequest(String reference, String claimantName, String note, String status) {
}
