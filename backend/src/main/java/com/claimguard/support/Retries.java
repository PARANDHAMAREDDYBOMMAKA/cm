package com.claimguard.support;

import java.time.Duration;
import java.util.function.Predicate;
import java.util.function.Supplier;

public final class Retries {

    private Retries() {
    }

    public static <T> T withBackoff(int attempts,
            Duration initialDelay,
            Predicate<RuntimeException> retryable,
            Supplier<T> action) {
        RuntimeException last = null;
        long delayMillis = initialDelay.toMillis();

        for (int attempt = 1; attempt <= attempts; attempt++) {
            try {
                return action.get();
            } catch (RuntimeException exception) {
                last = exception;
                if (attempt == attempts || !retryable.test(exception)) {
                    throw exception;
                }
                sleep(delayMillis);
                delayMillis *= 2;
            }
        }
        throw last;
    }

    private static void sleep(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException interrupted) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Interrupted while retrying", interrupted);
        }
    }
}
