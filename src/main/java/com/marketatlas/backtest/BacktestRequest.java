package com.marketatlas.backtest;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;

import java.math.BigDecimal;
import java.time.LocalDate;

public record BacktestRequest(
        @NotNull LocalDate startDate,
        @NotNull LocalDate endDate,
        @NotNull StrategyType strategy,
        @Min(1) @Max(400) Integer shortWindow,
        @Min(2) @Max(400) Integer longWindow,
        @NotNull @Positive @DecimalMax("1000000000000") BigDecimal initialCash,
        @PositiveOrZero @DecimalMax("1000000") BigDecimal commission,
        @Min(0) @Max(1000) Integer slippageBps,
        Boolean includeEquityCurve) {
}