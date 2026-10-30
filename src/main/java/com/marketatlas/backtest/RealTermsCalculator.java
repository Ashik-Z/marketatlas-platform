package com.marketatlas.backtest;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.NavigableMap;

/** Expresses backtest results in start-date dollars using monthly CPI. Pure; no I/O. */
public final class RealTermsCalculator {

    private static final BigDecimal HUNDRED = BigDecimal.valueOf(100);

    private RealTermsCalculator() {
    }

    public record RealTerms(
            LocalDate baseMonth,
            BigDecimal baseCpi,
            BigDecimal latestCpi,
            BigDecimal inflationPct,
            BigDecimal realFinalEquity,
            BigDecimal realTotalReturnPct,
            BigDecimal benchmarkRealFinalEquity,
            BigDecimal benchmarkRealTotalReturnPct,
            boolean cpiEstimated) {
    }

    /** Returns null when there is no CPI at or before the first bar's month. */
    public static RealTerms compute(List<Backtest.EquityPoint> strategy,
                                    List<Backtest.EquityPoint> benchmark,
                                    NavigableMap<LocalDate, BigDecimal> cpiByMonth) {
        if (cpiByMonth.isEmpty() || strategy.isEmpty() || benchmark.isEmpty()) {
            return null;
        }
        LocalDate firstMonth = strategy.getFirst().date().withDayOfMonth(1);
        LocalDate lastMonth = strategy.getLast().date().withDayOfMonth(1);
        Map.Entry<LocalDate, BigDecimal> base = cpiByMonth.floorEntry(firstMonth);
        Map.Entry<LocalDate, BigDecimal> latest = cpiByMonth.floorEntry(lastMonth);
        if (base == null || latest == null) {
            return null;
        }
        boolean estimated = !base.getKey().equals(firstMonth) || !latest.getKey().equals(lastMonth);

        BigDecimal realFinal = deflate(strategy.getLast().equity(), base.getValue(), latest.getValue());
        BigDecimal benchFinal = deflate(benchmark.getLast().equity(), base.getValue(), latest.getValue());
        return new RealTerms(
                firstMonth, base.getValue(), latest.getValue(),
                returnPct(base.getValue(), latest.getValue()),
                realFinal, returnPct(strategy.getFirst().equity(), realFinal),
                benchFinal, returnPct(benchmark.getFirst().equity(), benchFinal),
                estimated);
    }

    private static BigDecimal deflate(BigDecimal equity, BigDecimal baseCpi, BigDecimal cpi) {
        return equity.multiply(baseCpi).divide(cpi, 6, RoundingMode.HALF_EVEN);
    }

    private static BigDecimal returnPct(BigDecimal start, BigDecimal end) {
        if (start.signum() == 0) {
            return null;
        }
        return end.divide(start, 10, RoundingMode.HALF_EVEN)
                .subtract(BigDecimal.ONE)
                .multiply(HUNDRED)
                .setScale(4, RoundingMode.HALF_EVEN);
    }
}