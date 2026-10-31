package com.marketatlas.ui.api;

/** A failed call to the MarketAtlas API, already reduced to a safe, user-presentable kind. */
public class ApiException extends RuntimeException {

    public enum Kind { INVALID_INPUT, DUPLICATE_USER, INVALID_CREDENTIALS, RATE_LIMITED, UNAVAILABLE, NOT_FOUND, CONFLICT }

    private final Kind kind;
    private final Long retryAfterSeconds;

    public ApiException(Kind kind, String message, Long retryAfterSeconds) {
        super(message);
        this.kind = kind;
        this.retryAfterSeconds = retryAfterSeconds;
    }

    public Kind kind() {
        return kind;
    }

    /** Seconds to wait before retrying; only set for RATE_LIMITED when the API said so. */
    public Long retryAfterSeconds() {
        return retryAfterSeconds;
    }
}