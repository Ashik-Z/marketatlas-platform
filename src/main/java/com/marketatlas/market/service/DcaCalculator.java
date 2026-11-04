package com.marketatlas.market.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.NavigableMap;

/**
 * Dollar-cost averaging: buy a fixed amount on the first usable bar of each calendar month and value the
 * units held at the last usable bar. A bar is usable when it has a positive close and a CPI month at or
 * before it. Contributions and the final value are also expressed in base-month dollars.
 */
public final class DcaCalculator {

    private static final int UNIT_SCALE = 8;

    public record Purchase(LocalDate date, BigDecimal close, BigDecimal units, BigDecimal amount,
                           BigDecimal realAmount, boolean cpiEstimated) {}

    public record Result(List<Purchase> purchases, BigDecimal investedNominal, BigDecimal investedReal,
                         BigDecimal units, LocalDate valuationDate, BigDecimal valuationClose,
                         BigDecimal valueNominal, BigDecimal valueReal,
                         BigDecimal nominalGain, BigDecimal realGain,
                         BigDecimal nominalGainPct, BigDecimal realGainPct) {}

    private DcaCalculator() {
    }

    public static Result run(List<RealPriceCalculator.Bar> bars, NavigableMap<LocalDate, BigDecimal> cpiByMonth,
                             LocalDate baseMonth, BigDecimal amount) {
        if (amount == null || amount.signum() <= 0) {
            throw new IllegalArgumentException("amount must be greater than zero");
        }
        BigDecimal baseCpi = cpiByMonth.get(baseMonth);
        if (baseCpi == null) {
            throw new IllegalArgumentException("No CPI value for base month " + baseMonth);
        }

        List<Purchase> purchases = new ArrayList<>();
        BigDecimal units = BigDecimal.ZERO;
        BigDecimal investedReal = BigDecimal.ZERO;
        LocalDate boughtMonth = null;
        RealPriceCalculator.Bar last = null;
        Map.Entry<LocalDate, BigDecimal> lastCpi = null;

        for (RealPriceCalculator.Bar bar : bars) {
            if (bar.close() == null || bar.close() <= 0) {
                continue;
            }
            LocalDate month = bar.date().withDayOfMonth(1);
            Map.Entry<LocalDate, BigDecimal> cpi = cpiByMonth.floorEntry(month);
            if (cpi == null) {
                continue;
            }
            last = bar;
            lastCpi = cpi;
            if (month.equals(boughtMonth)) {
                continue;
            }
            boughtMonth = month;
            BigDecimal close = BigDecimal.valueOf(bar.close());
            BigDecimal bought = amount.divide(close, UNIT_SCALE, RoundingMode.HALF_EVEN);
            BigDecimal real = amount.multiply(baseCpi).divide(cpi.getValue(), 6, RoundingMode.HALF_EVEN);
            purchases.add(new Purchase(bar.date(), close, bought, amount,
                    real.setScale(2, RoundingMode.HALF_EVEN), !cpi.getKey().equals(month)));
            units = units.add(bought);
            investedReal = investedReal.add(real);
        }
        if (purchases.isEmpty()) {
            throw new IllegalArgumentException("No usable price bars with CPI data in this date range");
        }

        BigDecimal investedNominal = amount.multiply(BigDecimal.valueOf(purchases.size())).setScale(2, RoundingMode.HALF_EVEN);
        BigDecimal lastClose = BigDecimal.valueOf(last.close());
        BigDecimal valueNominal = units.multiply(lastClose).setScale(2, RoundingMode.HALF_EVEN);
        BigDecimal valueReal = units.multiply(lastClose).multiply(baseCpi)
                .divide(lastCpi.getValue(), 2, RoundingMode.HALF_EVEN);
        BigDecimal realInvested = investedReal.setScale(2, RoundingMode.HALF_EVEN);
        BigDecimal nominalGain = valueNominal.subtract(investedNominal);
        BigDecimal realGain = valueReal.subtract(realInvested);
        return new Result(List.copyOf(purchases), investedNominal, realInvested, units, last.date(), lastClose,
                valueNominal, valueReal, nominalGain, realGain,
                pct(nominalGain, investedNominal), pct(realGain, realInvested));
    }

    private static BigDecimal pct(BigDecimal gain, BigDecimal invested) {
        if (invested.signum() == 0) {
            return null;
        }
        return gain.multiply(BigDecimal.valueOf(100)).divide(invested, 4, RoundingMode.HALF_EVEN);
    }
}
