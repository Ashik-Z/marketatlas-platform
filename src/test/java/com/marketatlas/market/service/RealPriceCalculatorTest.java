package com.marketatlas.market.service;

import com.marketatlas.market.service.RealPriceCalculator.Bar;
import com.marketatlas.market.service.RealPriceCalculator.Point;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.NavigableMap;
import java.util.TreeMap;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RealPriceCalculatorTest {

    private static final LocalDate JAN = LocalDate.of(2026, 1, 1);
    private static final LocalDate FEB = LocalDate.of(2026, 2, 1);

    private static NavigableMap<LocalDate, BigDecimal> cpi() {
        NavigableMap<LocalDate, BigDecimal> map = new TreeMap<>();
        map.put(JAN, new BigDecimal("100"));
        map.put(FEB, new BigDecimal("110"));
        return map;
    }

    private static Point single(LocalDate date, Double close, LocalDate baseMonth) {
        return RealPriceCalculator.adjust(List.of(new Bar(date, close)), cpi(), baseMonth).get(0);
    }

    @Test
    void earlierMonthIsScaledUpToBaseMonthDollars() {
        Point p = single(LocalDate.of(2026, 1, 15), 50.0, FEB);

        assertEquals(new BigDecimal("55.000000"), p.realClose());
        assertEquals(JAN, p.cpiMonth());
        assertFalse(p.cpiEstimated());
    }

    @Test
    void barInTheBaseMonthIsUnchanged() {
        Point p = single(LocalDate.of(2026, 2, 10), 50.0, FEB);

        assertEquals(new BigDecimal("50.000000"), p.realClose());
    }

    @Test
    void monthWithoutCpiCarriesTheLatestEarlierValueAndIsFlagged() {
        Point p = single(LocalDate.of(2026, 3, 20), 55.0, FEB);

        assertEquals(FEB, p.cpiMonth());
        assertTrue(p.cpiEstimated());
        assertEquals(new BigDecimal("55.000000"), p.realClose());
    }

    @Test
    void barBeforeTheFirstCpiMonthHasNoRealValue() {
        Point p = single(LocalDate.of(2025, 12, 31), 50.0, FEB);

        assertNull(p.realClose());
        assertNull(p.cpiMonth());
        assertFalse(p.cpiEstimated());
        assertEquals(new BigDecimal("50.0"), p.nominalClose());
    }

    @Test
    void missingBaseMonthIsRejected() {
        assertThrows(IllegalArgumentException.class,
                () -> single(LocalDate.of(2026, 1, 15), 50.0, LocalDate.of(2026, 6, 1)));
    }

    @Test
    void barWithoutCloseKeepsNullsInsteadOfZero() {
        Point p = single(LocalDate.of(2026, 1, 15), null, FEB);

        assertNull(p.nominalClose());
        assertNull(p.realClose());
    }

    @Test
    void roundsHalfEvenToSixDecimals() {
        NavigableMap<LocalDate, BigDecimal> map = new TreeMap<>();
        map.put(JAN, new BigDecimal("7"));
        map.put(FEB, new BigDecimal("3"));

        Point p = RealPriceCalculator.adjust(
                List.of(new Bar(LocalDate.of(2026, 1, 15), 10.0)), map, FEB).get(0);

        assertEquals(new BigDecimal("4.285714"), p.realClose());
    }
}