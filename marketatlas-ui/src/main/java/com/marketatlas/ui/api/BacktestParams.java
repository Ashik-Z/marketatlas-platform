package com.marketatlas.ui.api;

import java.math.BigDecimal;
import java.time.LocalDate;

/** Request body of POST /api/assets/{id}/backtest. Null optional fields use the API defaults. */
public record BacktestParams(
        LocalDate startDate,
        LocalDate endDate,
        String strategy,
        Integer shortWindow,
        Integer longWindow,
        BigDecimal initialCash,
        BigDecimal commission,
        Integer slippageBps,
        boolean includeEquityCurve) {
}