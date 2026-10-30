package com.marketatlas.backtest;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class StrategiesTest {

    private static List<Backtest.Bar> closes(int... values) {
        List<Backtest.Bar> bars = new ArrayList<>();
        for (int i = 0; i < values.length; i++) {
            BigDecimal price = BigDecimal.valueOf(values[i]);
            bars.add(new Backtest.Bar(LocalDate.of(2026, 1, 1).plusDays(i), price, price));
        }
        return bars;
    }

    @Test
    void smaCrossoverFollowsTheTwoAverages() {
        // closes 1,2,3,2,1 with windows 2/3:
        // i=1 not enough history; i=2 avg(2,3)=2.5 > avg(1,2,3)=2; i=3 2.5 > 2.333; i=4 1.5 < 2
        var strategy = Strategies.smaCrossover(2, 3);
        var bars = closes(1, 2, 3, 2, 1);

        assertFalse(strategy.wantsPosition(bars, 1));
        assertTrue(strategy.wantsPosition(bars, 2));
        assertTrue(strategy.wantsPosition(bars, 3));
        assertFalse(strategy.wantsPosition(bars, 4));
    }

    @Test
    void equalAveragesMeanStayOut() {
        assertFalse(Strategies.smaCrossover(2, 3).wantsPosition(closes(5, 5, 5, 5), 3));
    }

    @Test
    void invalidWindowsAreRejected() {
        assertThrows(IllegalArgumentException.class, () -> Strategies.smaCrossover(3, 3));
        assertThrows(IllegalArgumentException.class, () -> Strategies.smaCrossover(0, 2));
        assertThrows(IllegalArgumentException.class, () -> Strategies.smaCrossover(1, 401));
    }

    @Test
    void strategiesHaveStableNames() {
        assertEquals("BUY_AND_HOLD", Strategies.buyAndHold().name());
        assertEquals("SMA_CROSSOVER_2_3", Strategies.smaCrossover(2, 3).name());
    }
}