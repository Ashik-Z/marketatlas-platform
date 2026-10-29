package com.marketatlas.common.exception;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

class ProviderRateLimitHandlerTest {

    @Test
    void shouldAnswerARateLimitWith503AndRetryAfter() {
        var response = new GlobalExceptionHandler().handleProviderRateLimit(
                new ProviderRateLimitException("Twelve Data rate limit exceeded (HTTP 429).", 45));

        assertEquals(503, response.getStatusCode().value());
        assertEquals("45", response.getHeaders().getFirst("Retry-After"));
        assertEquals("Market Data Provider Rate Limit", response.getBody().getTitle());
        // only a generic message goes to the client, never the provider's text
        assertFalse(response.getBody().getDetail().contains("Twelve Data"));
    }
}