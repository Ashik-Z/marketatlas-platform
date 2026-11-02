package com.marketatlas.common.exception;

public class WatchlistLimitException extends RuntimeException {
    public WatchlistLimitException(String message) {
        super(message);
    }
}
