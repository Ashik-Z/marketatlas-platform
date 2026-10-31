package com.marketatlas.ui.api;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/** The parts of the backtest response the UI shows. */
public record BacktestResult(
        Long assetId,
        String assetName,
        String currency,
        String strategy,
        LocalDate firstBarDate,
        LocalDate lastBarDate,
        int barsUsed,
        int skippedBars,
        BigDecimal initialCash,
        Metrics metrics,
        Metrics benchmark,
        List<Trade> trades,
        List<CurvePoint> equityCurve,
        List<CurvePoint> benchmarkEquityCurve,
        RealTerms realTerms,
        String realTermsNote) {

    public record Metrics(BigDecimal totalReturnPct, BigDecimal annualizedReturnPct, BigDecimal maxDrawdownPct,
                          BigDecimal annualizedVolatilityPct, int trades, BigDecimal commissionPaid,
                          BigDecimal exposurePct, int rejectedOrders) {
    }

    public record Trade(LocalDate date, String side, BigDecimal quantity, BigDecimal price, BigDecimal commission) {
    }

    public record CurvePoint(LocalDate date, BigDecimal equity) {
    }

    public record RealTerms(String baseMonth, BigDecimal inflationPct, BigDecimal realFinalEquity,
                            BigDecimal realTotalReturnPct, BigDecimal benchmarkRealFinalEquity,
                            BigDecimal benchmarkRealTotalReturnPct, boolean cpiEstimated) {
    }
}