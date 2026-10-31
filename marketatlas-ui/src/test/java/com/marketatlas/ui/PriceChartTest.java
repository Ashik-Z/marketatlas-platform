package com.marketatlas.ui;

import com.marketatlas.ui.api.RealPrices;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class PriceChartTest {

    private static RealPrices with(List<RealPrices.Point> points) {
        return new RealPrices(7L, "Gold Spot", "USD", "CPIAUCNS", "CPI", "FRED", LocalDate.of(2024, 1, 1),
                new BigDecimal("308.417"), LocalDate.of(2024, 2, 1), BigDecimal.ONE, BigDecimal.ONE, points);
    }

    @Test
    void buildsDatesAndBothSeries() {
        RealPrices data = with(List.of(
                new RealPrices.Point(LocalDate.of(2024, 1, 2), new BigDecimal("2000.50"), new BigDecimal("2000.50"),
                        LocalDate.of(2024, 1, 1), false),
                new RealPrices.Point(LocalDate.of(2024, 1, 3), new BigDecimal("2010.00"), new BigDecimal("2001.25"),
                        LocalDate.of(2024, 1, 1), true)));

        assertEquals("{\"dates\":[\"2024-01-02\",\"2024-01-03\"],\"nominal\":[2000.50,2010.00],\"real\":[2000.50,2001.25]}",
                PriceChart.json(data));
    }

    @Test
    void missingValuesBecomeNullGaps() {
        RealPrices data = with(List.of(
                new RealPrices.Point(LocalDate.of(2024, 1, 2), new BigDecimal("1E+3"), null, LocalDate.of(2024, 1, 1), false)));

        assertEquals("{\"dates\":[\"2024-01-02\"],\"nominal\":[1000],\"real\":[null]}", PriceChart.json(data));
    }

    @Test
    void noPointsGivesEmptyArrays() {
        assertEquals("{\"dates\":[],\"nominal\":[],\"real\":[]}", PriceChart.json(with(List.of())));
    }
}