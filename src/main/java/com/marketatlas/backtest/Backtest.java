package com.marketatlas.backtest;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/** Plain data types for the backtest engine. No Spring, no database. */
public final class Backtest {

    private Backtest() {
    }

    /** One daily bar. Bars must be ascending by date with unique dates. */
    public record Bar(LocalDate date, BigDecimal open, BigDecimal close) {
    }

    public enum Side { BUY, SELL }

    public record Trade(LocalDate date, Side side, BigDecimal quantity, BigDecimal price, BigDecimal commission) {
    }

    public record EquityPoint(LocalDate date, BigDecimal equity) {
    }

    public record Params(BigDecimal initialCash, BigDecimal commission, int slippageBps, boolean wholeShares) {
        public Params {
            if (initialCash == null || initialCash.signum() <= 0) {
                throw new IllegalArgumentException("initialCash must be positive");
            }
            if (commission == null || commission.signum() < 0) {
                throw new IllegalArgumentException("commission must not be negative");
            }
            if (slippageBps < 0 || slippageBps > 1000) {
                throw new IllegalArgumentException("slippageBps must be between 0 and 1000");
            }
        }
    }

    public record Result(List<Trade> trades, List<EquityPoint> equityCurve, BigDecimal finalEquity,
                         BigDecimal totalCommission, int rejectedOrders) {
    }
}