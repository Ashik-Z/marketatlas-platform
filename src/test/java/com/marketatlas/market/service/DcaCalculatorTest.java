package com.marketatlas.market.service;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.NavigableMap;
import java.util.TreeMap;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DcaCalculatorTest {

    private static final LocalDate JAN = LocalDate.of(2025, 1, 1);
    private static final LocalDate FEB = LocalDate.of(2025, 2, 1);

    private static NavigableMap<LocalDate, BigDecimal> cpi(String jan, String feb) {
        NavigableMap<LocalDate, BigDecimal> m = new TreeMap<>();
        m.put(JAN, new BigDecimal(jan));
        m.put(FEB, new BigDecimal(feb));
        return m;
    }

    private static RealPriceCalculator.Bar bar(String date, double close) {
        return new RealPriceCalculator.Bar(LocalDate.parse(date), close);
    }

    private static void assertMoney(String expected, BigDecimal actual) {
        assertEquals(0, new BigDecimal(expected).compareTo(actual), "expected " + expected + " but was " + actual);
    }

    @Test
    void buysOnFirstUsableBarOfEachMonth() {
        var r = DcaCalculator.run(List.of(bar("2025-01-02", 10), bar("2025-01-03", 11), bar("2025-02-03", 20)),
                cpi("100", "100"), FEB, new BigDecimal("100"));

        assertEquals(2, r.purchases().size());
        assertEquals(LocalDate.parse("2025-01-02"), r.purchases().get(0).date());
        assertEquals(LocalDate.parse("2025-02-03"), r.purchases().get(1).date());
    }

    @Test
    void flatCpiGivesEqualNominalAndRealFigures() {
        var r = DcaCalculator.run(List.of(bar("2025-01-02", 10), bar("2025-02-03", 20)),
                cpi("100", "100"), FEB, new BigDecimal("100"));

        assertMoney("200", r.investedNominal());
        assertMoney("200", r.investedReal());
        assertMoney("300", r.valueNominal());
        assertMoney("300", r.valueReal());
    }

    @Test
    void risingPricesThatOnlyKeepUpWithInflationShowNoRealGain() {
        var r = DcaCalculator.run(List.of(bar("2025-01-02", 10), bar("2025-02-03", 20)),
                cpi("100", "200"), FEB, new BigDecimal("100"));

        assertMoney("200", r.investedNominal());
        assertMoney("300", r.valueNominal());
        assertMoney("100", r.nominalGain());
        assertMoney("50", r.nominalGainPct());
        assertMoney("300", r.investedReal());
        assertMoney("300", r.valueReal());
        assertMoney("0", r.realGain());
    }

    @Test
    void monthsBeforeTheFirstCpiMonthAreSkipped() {
        NavigableMap<LocalDate, BigDecimal> onlyFeb = new TreeMap<>();
        onlyFeb.put(FEB, new BigDecimal("100"));

        var r = DcaCalculator.run(List.of(bar("2025-01-02", 10), bar("2025-02-03", 20)),
                onlyFeb, FEB, new BigDecimal("100"));

        assertEquals(1, r.purchases().size());
        assertEquals(LocalDate.parse("2025-02-03"), r.purchases().get(0).date());
    }

    @Test
    void flagsPurchasesThatUseAnEarlierCpiMonth() {
        NavigableMap<LocalDate, BigDecimal> onlyJan = new TreeMap<>();
        onlyJan.put(JAN, new BigDecimal("100"));

        var r = DcaCalculator.run(List.of(bar("2025-01-02", 10), bar("2025-02-03", 20)),
                onlyJan, JAN, new BigDecimal("100"));

        assertTrue(!r.purchases().get(0).cpiEstimated());
        assertTrue(r.purchases().get(1).cpiEstimated());
    }

    @Test
    void rejectsBadInput() {
        var bars = List.of(bar("2025-01-02", 10));
        assertThrows(IllegalArgumentException.class,
                () -> DcaCalculator.run(bars, cpi("100", "100"), LocalDate.of(2030, 1, 1), BigDecimal.TEN));
        assertThrows(IllegalArgumentException.class,
                () -> DcaCalculator.run(bars, cpi("100", "100"), FEB, BigDecimal.ZERO));
        assertThrows(IllegalArgumentException.class,
                () -> DcaCalculator.run(List.of(), cpi("100", "100"), FEB, BigDecimal.TEN));
    }
}
