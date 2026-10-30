package com.marketatlas.backtest;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;

/** Performance figures for a finished backtest. Money stays BigDecimal; ratio metrics use double and are rounded. */
public final class BacktestMetrics {

    static final int MIN_DAYS_TO_ANNUALIZE = 90;
    private static final double DAYS_PER_YEAR = 365.0;
    private static final double TRADING_DAYS_PER_YEAR = 252.0;
    private static final BigDecimal HUNDRED = BigDecimal.valueOf(100);

    public record Metrics(
            BigDecimal totalReturnPct,
            BigDecimal annualizedReturnPct,
            BigDecimal maxDrawdownPct,
            LocalDate drawdownPeakDate,
            LocalDate drawdownTroughDate,
            BigDecimal annualizedVolatilityPct,
            int trades,
            BigDecimal commissionPaid,
            BigDecimal commissionPctOfCapital,
            BigDecimal exposurePct,
            int rejectedOrders) {
    }

    private BacktestMetrics() {
    }

    public static Metrics compute(Backtest.Result result) {
        List<Backtest.EquityPoint> curve = result.equityCurve();
        if (curve.isEmpty()) {
            throw new IllegalArgumentException("Equity curve is empty");
        }
        Backtest.EquityPoint first = curve.getFirst();
        Backtest.EquityPoint last = curve.getLast();
        BigDecimal initial = first.equity();

        BigDecimal totalReturn = pct(last.equity().divide(initial, 10, RoundingMode.HALF_EVEN)
                .subtract(BigDecimal.ONE));

        BigDecimal annualized = null;
        long days = ChronoUnit.DAYS.between(first.date(), last.date());
        if (days >= MIN_DAYS_TO_ANNUALIZE) {
            double growth = last.equity().divide(initial, 10, RoundingMode.HALF_EVEN).doubleValue();
            annualized = BigDecimal.valueOf((Math.pow(growth, DAYS_PER_YEAR / days) - 1) * 100)
                    .setScale(4, RoundingMode.HALF_EVEN);
        }

        // Max drawdown: largest fall from a running peak.
        BigDecimal peak = initial;
        LocalDate peakDate = first.date();
        BigDecimal maxDrawdown = BigDecimal.ZERO;
        LocalDate ddPeak = null;
        LocalDate ddTrough = null;
        for (Backtest.EquityPoint point : curve) {
            if (point.equity().compareTo(peak) > 0) {
                peak = point.equity();
                peakDate = point.date();
            } else {
                BigDecimal drawdown = peak.subtract(point.equity()).divide(peak, 10, RoundingMode.HALF_EVEN);
                if (drawdown.compareTo(maxDrawdown) > 0) {
                    maxDrawdown = drawdown;
                    ddPeak = peakDate;
                    ddTrough = point.date();
                }
            }
        }

        BigDecimal commissionPct = result.totalCommission().divide(initial, 10, RoundingMode.HALF_EVEN)
                .multiply(HUNDRED).setScale(4, RoundingMode.HALF_EVEN);

        return new Metrics(totalReturn, annualized, pct(maxDrawdown), ddPeak, ddTrough,
                volatility(curve), result.trades().size(), result.totalCommission(), commissionPct,
                exposure(result), result.rejectedOrders());
    }

    private static BigDecimal pct(BigDecimal fraction) {
        return fraction.multiply(HUNDRED).setScale(4, RoundingMode.HALF_EVEN);
    }

    /** Annualised sample standard deviation of simple returns between consecutive equity points. */
    private static BigDecimal volatility(List<Backtest.EquityPoint> curve) {
        int count = curve.size() - 1;
        if (count < 2) {
            return null;
        }
        double[] returns = new double[count];
        double sum = 0;
        for (int i = 1; i < curve.size(); i++) {
            returns[i - 1] = curve.get(i).equity().doubleValue() / curve.get(i - 1).equity().doubleValue() - 1;
            sum += returns[i - 1];
        }
        double mean = sum / count;
        double squares = 0;
        for (double r : returns) {
            squares += (r - mean) * (r - mean);
        }
        double std = Math.sqrt(squares / (count - 1));
        return BigDecimal.valueOf(std * Math.sqrt(TRADING_DAYS_PER_YEAR) * 100).setScale(4, RoundingMode.HALF_EVEN);
    }

    /** Share of equity points on which a position was held at the close. */
    private static BigDecimal exposure(Backtest.Result result) {
        List<Backtest.EquityPoint> curve = result.equityCurve();
        List<Backtest.Trade> trades = result.trades();
        int invested = 0;
        int next = 0;
        boolean holding = false;
        for (Backtest.EquityPoint point : curve) {
            while (next < trades.size() && !trades.get(next).date().isAfter(point.date())) {
                holding = trades.get(next).side() == Backtest.Side.BUY;
                next++;
            }
            if (holding) {
                invested++;
            }
        }
        return BigDecimal.valueOf(invested).divide(BigDecimal.valueOf(curve.size()), 10, RoundingMode.HALF_EVEN)
                .multiply(HUNDRED).setScale(4, RoundingMode.HALF_EVEN);
    }
}