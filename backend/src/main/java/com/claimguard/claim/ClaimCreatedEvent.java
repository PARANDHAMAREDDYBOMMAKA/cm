package com.claimguard.claim;

import java.util.UUID;

public record ClaimCreatedEvent(UUID claimId, String reference) {
}
