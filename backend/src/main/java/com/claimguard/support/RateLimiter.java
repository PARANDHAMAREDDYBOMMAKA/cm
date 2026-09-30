package com.claimguard.support;

public interface RateLimiter {

    boolean tryAcquire(String key);
}
