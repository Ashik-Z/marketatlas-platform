package com.marketatlas.common.ratelimit;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Clock;
import java.time.Duration;

@Configuration
public class RateLimitConfig {

    @Bean
    RateLimiter orderRateLimiter(Clock clock,
                                 @Value("${marketatlas.rate-limit.orders-per-minute:30}") int ordersPerMinute) {
        return new RateLimiter(ordersPerMinute, Duration.ofMinutes(1), 100_000, clock);
    }

    @Bean
    RateLimiter backtestRateLimiter(Clock clock,
                                    @Value("${marketatlas.rate-limit.backtests-per-minute:10}") int backtestsPerMinute) {
        return new RateLimiter(backtestsPerMinute, Duration.ofMinutes(1), 100_000, clock);
    }
}