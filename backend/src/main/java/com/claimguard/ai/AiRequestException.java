package com.claimguard.ai;

public class AiRequestException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    private final int status;

    public AiRequestException(String message, Throwable cause) {
        super(message, cause);
        this.status = 0;
    }

    public AiRequestException(String message) {
        this(message, 0);
    }

    public AiRequestException(String message, int status) {
        super(message);
        this.status = status;
    }

    public int status() {
        return status;
    }

    public boolean isRetryable() {
        return status == 408 || status == 429 || status >= 500;
    }
}
