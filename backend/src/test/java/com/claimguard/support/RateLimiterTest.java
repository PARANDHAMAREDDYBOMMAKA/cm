package com.claimguard.support;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class RateLimiterTest {

    @Test
    void allowsTheBurstThenRefusesFurtherRequests() {
        RateLimiter limiter = new RateLimiter(3, 1);

        assertThat(limiter.tryAcquire("alice")).isTrue();
        assertThat(limiter.tryAcquire("alice")).isTrue();
        assertThat(limiter.tryAcquire("alice")).isTrue();
        assertThat(limiter.tryAcquire("alice")).isFalse();
    }

    @Test
    void bucketsAreHeldPerSubject() {
        RateLimiter limiter = new RateLimiter(1, 1);

        assertThat(limiter.tryAcquire("alice")).isTrue();
        assertThat(limiter.tryAcquire("alice")).isFalse();
        assertThat(limiter.tryAcquire("bob")).isTrue();
    }

    @Test
    void unauthenticatedCallersShareOneBucketRatherThanBypassingTheLimit() {
        RateLimiter limiter = new RateLimiter(1, 1);

        assertThat(limiter.tryAcquire(null)).isTrue();
        assertThat(limiter.tryAcquire(null)).isFalse();
    }

    @Test
    void refillsOverTime() throws InterruptedException {
        RateLimiter limiter = new RateLimiter(1, 60_000);

        assertThat(limiter.tryAcquire("alice")).isTrue();
        assertThat(limiter.tryAcquire("alice")).isFalse();

        Thread.sleep(5);

        assertThat(limiter.tryAcquire("alice")).isTrue();
    }
}
