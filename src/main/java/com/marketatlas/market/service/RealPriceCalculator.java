package com.marketatlas.market.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.NavigableMap;

/**
 * Converts nominal closes into "base month" dollars using a monthly CPI series.
 * CPI is published monthly and with a lag, so a bar whose month has no CPI yet uses the latest earlier
 * month and is flagged as estimated; a bar before the first CPI month gets no real value at all.
 */
public final class RealPriceCalculator {

    private static final int SCALE = 6;

    public record Bar(LocalDate date, Double close) {}

    public record Point(LocalDate date, BigDecimal nominalClose, BigDecimal realClose,
                        LocalDate cpiMonth, boolean cpiEstimated) {}

    private RealPriceCalculator() {
    }

    /** cpiByMonth is keyed by the first day of each month and holds only real (non-null) values. */
    public static List<Point> adjust(List<Bar> bars, NavigableMap<LocalDate, BigDecimal> cpiByMonth,
                                     LocalDate baseMonth) {
        BigDecimal baseCpi = cpiByMonth.get(baseMonth);
        if (baseCpi == null) {
            throw new IllegalArgumentException("No CPI value for base month " + baseMonth);
        }

        List<Point> points = new ArrayList<>(bars.size());
        for (Bar bar : bars) {
            BigDecimal nominal = bar.close() == null ? null : BigDecimal.valueOf(bar.close());
            LocalDate month = bar.date().withDayOfMonth(1);
            Map.Entry<LocalDate, BigDecimal> cpi = cpiByMonth.floorEntry(month);

            if (cpi == null) {
                points.add(new Point(bar.date(), nominal, null, null, false));
                continue;
            }
            BigDecimal real = nominal == null ? null
                    : nominal.multiply(baseCpi).divide(cpi.getValue(), SCALE, RoundingMode.HALF_EVEN);
            points.add(new Point(bar.date(), nominal, real, cpi.getKey(), !cpi.getKey().equals(month)));
        }
        return points;
    }
}