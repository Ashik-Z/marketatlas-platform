package com.marketatlas.backtest;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.TreeMap;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RealTermsCalculatorTest {

    private static final TreeMap<LocalDate, BigDecimal> CPI = new TreeMap<>();

    static {
        CPI.put(LocalDate.of(2024, 1, 1), new BigDecimal("100"));
        CPI.put(LocalDate.of(2024, 2, 1), new BigDecimal("110"));
    }

    private static Backtest.EquityPoint point(LocalDate date, String equity) {
        return new Backtest.EquityPoint(date, new BigDecimal(equity));
    }

    private static void assertNumber(String expected, BigDecimal actual) {
        assertNotNull(actual);
        assertEquals(0, new BigDecimal(expected).compareTo(actual), "expected " + expected + " but was " + actual);
    }

    @Test
    void deflatesToStartDateDollars() {
        var strategy = List.of(point(LocalDate.of(2024, 1, 15), "1000"), point(LocalDate.of(2024, 2, 20), "1210"));
        var benchmark = List.of(point(LocalDate.of(2024, 1, 15), "1000"), point(LocalDate.of(2024, 2, 20), "1100"));

        var terms = RealTermsCalculator.compute(strategy, benchmark, CPI);

        assertEquals(LocalDate.of(2024, 1, 1), terms.baseMonth());
        assertNumber("100", terms.baseCpi());
        assertNumber("110", terms.latestCpi());
        assertNumber("10", terms.inflationPct());
        assertNumber("1100", terms.realFinalEquity());
        assertNumber("10", terms.realTotalReturnPct());
        assertNumber("1000", terms.benchmarkRealFinalEquity());
        assertNumber("0", terms.benchmarkRealTotalReturnPct());
        assertFalse(terms.cpiEstimated());
    }

    @Test
    void monthWithoutCpiReusesLatestEarlierValueAndIsFlagged() {
        var strategy = List.of(point(LocalDate.of(2024, 1, 15), "1000"), point(LocalDate.of(2024, 3, 5), "1210"));

        var terms = RealTermsCalculator.compute(strategy, strategy, CPI);

        assertNumber("110", terms.latestCpi());
        assertNumber("1100", terms.realFinalEquity());
        assertTrue(terms.cpiEstimated());
    }

    @Test
    void noCpiAtOrBeforeTheStartGivesNull() {
        var strategy = List.of(point(LocalDate.of(2023, 12, 15), "1000"), point(LocalDate.of(2024, 2, 20), "1210"));

        assertNull(RealTermsCalculator.compute(strategy, strategy, CPI));
    }

    @Test
    void emptyCpiGivesNull() {
        var strategy = List.of(point(LocalDate.of(2024, 1, 15), "1000"), point(LocalDate.of(2024, 2, 20), "1210"));

        assertNull(RealTermsCalculator.compute(strategy, strategy, new TreeMap<>()));
    }

    @Test
    void lossInRealTermsWhileFlatInNominalTerms() {
        var flat = List.of(point(LocalDate.of(2024, 1, 15), "1000"), point(LocalDate.of(2024, 2, 20), "1000"));

        var terms = RealTermsCalculator.compute(flat, flat, CPI);

        assertNumber("0", terms.realTotalReturnPct().subtract(terms.realTotalReturnPct()));
        assertNumber("909.090909", terms.realFinalEquity());
        assertNumber("-9.0909", terms.realTotalReturnPct());
    }
}