package com.marketatlas.backtest;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

public final class Strategies {

    private static final int MAX_WINDOW = 400;

    private Strategies() {
    }

    public static Strategy buyAndHold() {
        return new Strategy() {
            @Override
            public boolean wantsPosition(List<Backtest.Bar> bars, int index) {
                return true;
            }

            @Override
            public String name() {
                return "BUY_AND_HOLD";
            }
        };
    }

    /** Invested while the short moving average of closes is above the long one; flat otherwise or without enough history. */
    public static Strategy smaCrossover(int shortWindow, int longWindow) {
        if (shortWindow < 1 || longWindow <= shortWindow || longWindow > MAX_WINDOW) {
            throw new IllegalArgumentException(
                    "windows must satisfy 1 <= short < long <= " + MAX_WINDOW);
        }
        return new Strategy() {
            @Override
            public boolean wantsPosition(List<Backtest.Bar> bars, int index) {
                if (index + 1 < longWindow) {
                    return false;
                }
                return average(bars, index, shortWindow).compareTo(average(bars, index, longWindow)) > 0;
            }

            @Override
            public String name() {
                return "SMA_CROSSOVER_" + shortWindow + "_" + longWindow;
            }
        };
    }

    private static BigDecimal average(List<Backtest.Bar> bars, int endIndex, int window) {
        BigDecimal sum = BigDecimal.ZERO;
        for (int i = endIndex - window + 1; i <= endIndex; i++) {
            sum = sum.add(bars.get(i).close());
        }
        return sum.divide(BigDecimal.valueOf(window), 10, RoundingMode.HALF_EVEN);
    }
}