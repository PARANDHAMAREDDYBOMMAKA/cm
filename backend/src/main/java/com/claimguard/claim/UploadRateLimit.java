package com.claimguard.claim;

import com.claimguard.access.AccessPolicy;
import com.claimguard.support.RateLimiter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;

@Component
public class UploadRateLimit {

    private final AccessPolicy access;
    private final RateLimiter limiter;

    public UploadRateLimit(AccessPolicy access,
            @Value("${UPLOAD_BURST:10}") int burst,
            @Value("${UPLOAD_PER_MINUTE:30}") int perMinute) {
        this.access = access;
        this.limiter = new RateLimiter(burst, perMinute);
    }

    public void check() {
        String subject = access.ownerSubject();
        if (!limiter.tryAcquire(subject)) {
            throw new ResponseStatusException(HttpStatus.TOO_MANY_REQUESTS,
                    "Too many uploads. Wait a moment and try again.");
        }
    }
}
