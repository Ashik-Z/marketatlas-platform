package com.marketatlas.backtest;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

class BacktestMetricsTest {

    private static final LocalDate BASE = LocalDate.of(2024, 1, 1);

    private static Backtest.Result result(List<Backtest.Trade> trades, String commission, int rejected,
                                          Object... dayAndEquity) {
        List<Backtest.EquityPoint> curve = new ArrayList<>();
        for (int i = 0; i < dayAndEquity.length; i += 2) {
            curve.add(new Backtest.EquityPoint(BASE.plusDays((int) dayAndEquity[i]),
                    new BigDecimal((String) dayAndEquity[i + 1])));
        }
        return new Backtest.Result(trades, curve, curve.getLast().equity(), new BigDecimal(commission), rejected);
    }

    private static Backtest.Result curve(Object... dayAndEquity) {
        return result(List.of(), "0", 0, dayAndEquity);
    }

    private static Backtest.Trade trade(int day, Backtest.Side side) {
        return new Backtest.Trade(BASE.plusDays(day), side, BigDecimal.ONE, BigDecimal.ONE, BigDecimal.ZERO);
    }

    @Test
    void totalReturnAndMaxDrawdownWithTheirDates() {
        // 1000 -> 1200 is +20%. Peak 1100 (day 1) to trough 990 (day 2): (1100-990)/1100 = 10%.
        var m = BacktestMetrics.compute(curve(0, "1000", 1, "1100", 2, "990", 3, "1200"));

        assertEquals(new BigDecimal("20.0000"), m.totalReturnPct());
        assertEquals(new BigDecimal("10.0000"), m.maxDrawdownPct());
        assertEquals(BASE.plusDays(1), m.drawdownPeakDate());
        assertEquals(BASE.plusDays(2), m.drawdownTroughDate());
    }

    @Test
    void aRisingCurveHasNoDrawdown() {
        var m = BacktestMetrics.compute(curve(0, "100", 1, "110", 2, "120"));

        assertEquals(new BigDecimal("0.0000"), m.maxDrawdownPct());
        assertNull(m.drawdownPeakDate());
        assertNull(m.drawdownTroughDate());
    }

    @Test
    void annualizedReturnUsesA365DayYear() {
        // 1000 -> 1210 over 730 days = 2 years: sqrt(1.21) - 1 = 10% a year
        var m = BacktestMetrics.compute(curve(0, "1000", 730, "1210"));

        assertEquals(new BigDecimal("10.0000"), m.annualizedReturnPct());
        assertNull(m.annualizedVolatilityPct());
    }

    @Test
    void shortSpansAreNotAnnualized() {
        assertNull(BacktestMetrics.compute(curve(0, "1000", 89, "1100")).annualizedReturnPct());
        assertNotNull(BacktestMetrics.compute(curve(0, "1000", 90, "1100")).annualizedReturnPct());
    }

    @Test
    void volatilityIsTheAnnualizedSampleStandardDeviationOfDailyReturns() {
        // 100 -> 110 -> 99: returns +10% and -10%; sample std = sqrt(0.02); x sqrt(252) x 100 = 224.4994%
        var m = BacktestMetrics.compute(curve(0, "100", 1, "110", 2, "99"));

        assertEquals(new BigDecimal("224.4994"), m.annualizedVolatilityPct());
    }

    @Test
    void exposureCountsDaysWithAPositionAtTheClose() {
        // BUY fills at day 1's open, SELL at day 3's open: held at the close of days 1 and 2 -> 2 of 5 = 40%
        var closed = BacktestMetrics.compute(result(
                List.of(trade(1, Backtest.Side.BUY), trade(3, Backtest.Side.SELL)), "0", 0,
                0, "1000", 1, "1000", 2, "1000", 3, "1000", 4, "1000"));
        assertEquals(new BigDecimal("40.0000"), closed.exposurePct());

        // never sold: days 1..4 -> 4 of 5 = 80%
        var open = BacktestMetrics.compute(result(
                List.of(trade(1, Backtest.Side.BUY)), "0", 0,
                0, "1000", 1, "1000", 2, "1000", 3, "1000", 4, "1000"));
        assertEquals(new BigDecimal("80.0000"), open.exposurePct());

        assertEquals(new BigDecimal("0.0000"),
                BacktestMetrics.compute(curve(0, "1000", 1, "1000")).exposurePct());
    }

    @Test
    void costsAndCountsArePassedThrough() {
        // commission 4 on 1000 starting capital = 0.4%
        var m = BacktestMetrics.compute(result(
                List.of(trade(1, Backtest.Side.BUY), trade(3, Backtest.Side.SELL)), "4", 3,
                0, "1000", 4, "1000"));

        assertEquals(2, m.trades());
        assertEquals(3, m.rejectedOrders());
        assertEquals(0, new BigDecimal("4").compareTo(m.commissionPaid()));
        assertEquals(new BigDecimal("0.4000"), m.commissionPctOfCapital());
    }

    @Test
    void anEmptyCurveIsRejected() {
        var empty = new Backtest.Result(List.of(), List.of(), BigDecimal.ZERO, BigDecimal.ZERO, 0);

        assertThrows(IllegalArgumentException.class, () -> BacktestMetrics.compute(empty));
    }
}