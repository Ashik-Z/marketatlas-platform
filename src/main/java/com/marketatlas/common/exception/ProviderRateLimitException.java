package com.marketatlas.common.exception;

public class ProviderRateLimitException extends MarketDataProviderException {

    private static final int DEFAULT_RETRY_AFTER_SECONDS = 60;
    private static final int MAX_RETRY_AFTER_SECONDS = 3600;

    private final int retryAfterSeconds;

    public ProviderRateLimitException(String message, int retryAfterSeconds) {
        super(message);
        this.retryAfterSeconds = retryAfterSeconds;
    }

    public int getRetryAfterSeconds() {
        return retryAfterSeconds;
    }

    /** Reads a Retry-After header given in seconds; anything else falls back to one minute. */
    public static int parseRetryAfter(String headerValue) {
        if (headerValue == null) {
            return DEFAULT_RETRY_AFTER_SECONDS;
        }
        try {
            int seconds = Integer.parseInt(headerValue.trim());
            return Math.min(Math.max(seconds, 1), MAX_RETRY_AFTER_SECONDS);
        } catch (NumberFormatException exception) {
            return DEFAULT_RETRY_AFTER_SECONDS;
        }
    }
}