package com.claimguard.claim;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

import java.util.UUID;

@ResponseStatus(HttpStatus.NOT_FOUND)
public class ClaimNotFoundException extends RuntimeException {

    public ClaimNotFoundException(UUID id) {
        super("Claim not found: " + id);
    }
}
