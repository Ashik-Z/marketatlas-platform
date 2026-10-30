package com.marketatlas.backtest;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public record BacktestResponse(
        Long assetId,
        String assetName,
        String currency,
        String strategy,
        LocalDate firstBarDate,
        LocalDate lastBarDate,
        int barsUsed,
        int skippedBars,
        BigDecimal initialCash,
        BigDecimal commission,
        int slippageBps,
        BacktestMetrics.Metrics metrics,
        BacktestMetrics.Metrics benchmark,
        List<TradeView> trades,
        List<CurvePoint> equityCurve,
        List<CurvePoint> benchmarkEquityCurve) {

    public record TradeView(LocalDate date, String side, BigDecimal quantity, BigDecimal price,
                            BigDecimal commission) {
    }

    public record CurvePoint(LocalDate date, BigDecimal equity) {
    }
}