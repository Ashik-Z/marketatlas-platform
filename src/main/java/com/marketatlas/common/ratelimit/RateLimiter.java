package com.marketatlas.common.ratelimit;

import java.time.Clock;
import java.time.Duration;
import java.util.concurrent.ConcurrentHashMap;

/** Fixed-window counter per key, in memory. Fails closed when too many distinct keys are tracked. */
public final class RateLimiter {

    private record Window(long startMillis, int count) {}

    private final int limit;
    private final long windowMillis;
    private final int maxKeys;
    private final Clock clock;
    private final ConcurrentHashMap<String, Window> windows = new ConcurrentHashMap<>();

    public RateLimiter(int limit, Duration window, int maxKeys, Clock clock) {
        if (limit < 1 || window.isZero() || window.isNegative() || maxKeys < 1) {
            throw new IllegalArgumentException("Invalid rate limiter settings");
        }
        this.limit = limit;
        this.windowMillis = window.toMillis();
        this.maxKeys = maxKeys;
        this.clock = clock;
    }

    /** Counts one hit. Returns false when this hit is over the limit (or the key table is full). */
    public boolean tryAcquire(String key) {
        long now = clock.millis();
        if (windows.size() >= maxKeys && !windows.containsKey(key)) {
            purgeExpired(now);
            if (windows.size() >= maxKeys) {
                return false;
            }
        }
        boolean[] allowed = {true};
        windows.compute(key, (k, w) -> {
            Window current = (w == null || now - w.startMillis >= windowMillis) ? new Window(now, 0) : w;
            if (current.count >= limit) {
                allowed[0] = false;
                return current;
            }
            return new Window(current.startMillis, current.count + 1);
        });
        return allowed[0];
    }

    /** Counts one hit without checking; used to record failures. */
    public void record(String key) {
        tryAcquire(key);
    }

    /** True when the key has used up its limit in the current window. Does not count a hit. */
    public boolean isBlocked(String key) {
        Window w = windows.get(key);
        long now = clock.millis();
        return w != null && now - w.startMillis < windowMillis && w.count >= limit;
    }

    public long retryAfterSeconds(String key) {
        Window w = windows.get(key);
        if (w == null) {
            return 1;
        }
        long remaining = windowMillis - (clock.millis() - w.startMillis);
        return Math.max(1, (remaining + 999) / 1000);
    }

    private void purgeExpired(long now) {
        windows.entrySet().removeIf(e -> now - e.getValue().startMillis >= windowMillis);
    }
}