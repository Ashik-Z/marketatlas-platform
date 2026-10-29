package com.marketatlas.common.ratelimit;

import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RateLimiterTest {

    private static final class MutableClock extends Clock {
        private Instant now = Instant.parse("2026-01-01T00:00:00Z");

        void advance(Duration d) { now = now.plus(d); }

        @Override public java.time.ZoneId getZone() { return ZoneOffset.UTC; }
        @Override public Clock withZone(java.time.ZoneId zone) { return this; }
        @Override public Instant instant() { return now; }
    }

    private final MutableClock clock = new MutableClock();

    @Test
    void allowsUpToTheLimitThenBlocks() {
        RateLimiter limiter = new RateLimiter(3, Duration.ofMinutes(1), 100, clock);

        assertTrue(limiter.tryAcquire("a"));
        assertTrue(limiter.tryAcquire("a"));
        assertTrue(limiter.tryAcquire("a"));
        assertFalse(limiter.tryAcquire("a"));
        assertTrue(limiter.isBlocked("a"));
    }

    @Test
    void keysAreIndependent() {
        RateLimiter limiter = new RateLimiter(1, Duration.ofMinutes(1), 100, clock);

        assertTrue(limiter.tryAcquire("a"));
        assertFalse(limiter.tryAcquire("a"));
        assertTrue(limiter.tryAcquire("b"));
    }

    @Test
    void windowResetsAfterItExpires() {
        RateLimiter limiter = new RateLimiter(1, Duration.ofMinutes(1), 100, clock);

        assertTrue(limiter.tryAcquire("a"));
        assertFalse(limiter.tryAcquire("a"));
        clock.advance(Duration.ofSeconds(61));

        assertFalse(limiter.isBlocked("a"));
        assertTrue(limiter.tryAcquire("a"));
    }

    @Test
    void isBlockedDoesNotCountAHit() {
        RateLimiter limiter = new RateLimiter(2, Duration.ofMinutes(1), 100, clock);

        for (int i = 0; i < 10; i++) {
            assertFalse(limiter.isBlocked("a"));
        }
        limiter.record("a");
        assertFalse(limiter.isBlocked("a"));
        limiter.record("a");
        assertTrue(limiter.isBlocked("a"));
    }

    @Test
    void retryAfterCountsDownAndRoundsUp() {
        RateLimiter limiter = new RateLimiter(1, Duration.ofSeconds(60), 100, clock);
        limiter.tryAcquire("a");

        assertEquals(60, limiter.retryAfterSeconds("a"));
        clock.advance(Duration.ofMillis(30_500));
        assertEquals(30, limiter.retryAfterSeconds("a"));
    }

    @Test
    void failsClosedWhenTheKeyTableIsFull() {
        RateLimiter limiter = new RateLimiter(5, Duration.ofMinutes(1), 2, clock);

        assertTrue(limiter.tryAcquire("a"));
        assertTrue(limiter.tryAcquire("b"));
        assertFalse(limiter.tryAcquire("c"));
        assertTrue(limiter.tryAcquire("a"));

        clock.advance(Duration.ofMinutes(2));
        assertTrue(limiter.tryAcquire("c"));
    }

    @Test
    void rejectsInvalidSettings() {
        assertThrows(IllegalArgumentException.class,
                () -> new RateLimiter(0, Duration.ofMinutes(1), 10, clock));
        assertThrows(IllegalArgumentException.class,
                () -> new RateLimiter(1, Duration.ZERO, 10, clock));
    }
}