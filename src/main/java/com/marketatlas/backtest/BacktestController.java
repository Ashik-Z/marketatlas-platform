package com.marketatlas.backtest;

import com.marketatlas.common.exception.RateLimitExceededException;
import com.marketatlas.common.ratelimit.RateLimiter;
import com.marketatlas.trading.service.Caller;
import jakarta.validation.Valid;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/assets/{assetId}/backtest")
public class BacktestController {

    private final BacktestService backtestService;
    private final RateLimiter backtestRateLimiter;

    public BacktestController(BacktestService backtestService, RateLimiter backtestRateLimiter) {
        this.backtestService = backtestService;
        this.backtestRateLimiter = backtestRateLimiter;
    }

    @PostMapping
    public BacktestResponse run(@PathVariable Long assetId,
                                @Valid @RequestBody BacktestRequest request,
                                Authentication authentication) {
        Caller caller = Caller.from(authentication);
        if (!backtestRateLimiter.tryAcquire(caller.username())) {
            throw new RateLimitExceededException(backtestRateLimiter.retryAfterSeconds(caller.username()));
        }
        return backtestService.run(assetId, request);
    }
}