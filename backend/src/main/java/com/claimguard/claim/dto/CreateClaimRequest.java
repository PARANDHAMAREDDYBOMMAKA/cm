package com.claimguard.claim.dto;

public record CreateClaimRequest(String reference, String claimantName, String note) {
}
