package com.marketatlas.backtest;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * Long-only, daily-bar backtest. A decision made after the close of bar i is executed at the open of bar i+1,
 * so no decision can use a price from the day it fills. The decision after the last bar is never executed.
 */
public final class BacktestEngine {

    private static final int MONEY_SCALE = 6;
    private static final BigDecimal TEN_THOUSAND = BigDecimal.valueOf(10_000);

    private BacktestEngine() {
    }

    public static Backtest.Result run(List<Backtest.Bar> bars, Strategy strategy, Backtest.Params params) {
        validate(bars);

        BigDecimal slippage = BigDecimal.valueOf(params.slippageBps())
                .divide(TEN_THOUSAND, 10, RoundingMode.HALF_EVEN);
        BigDecimal buyFactor = BigDecimal.ONE.add(slippage);
        BigDecimal sellFactor = BigDecimal.ONE.subtract(slippage);

        BigDecimal cash = params.initialCash();
        BigDecimal shares = BigDecimal.ZERO;
        BigDecimal totalCommission = BigDecimal.ZERO;
        int rejected = 0;
        List<Backtest.Trade> trades = new ArrayList<>();
        List<Backtest.EquityPoint> curve = new ArrayList<>(bars.size());
        Boolean wanted = null;

        for (int i = 0; i < bars.size(); i++) {
            Backtest.Bar bar = bars.get(i);
            boolean holding = shares.signum() > 0;

            if (wanted != null && wanted != holding) {
                if (wanted) {
                    BigDecimal price = bar.open().multiply(buyFactor).setScale(MONEY_SCALE, RoundingMode.HALF_EVEN);
                    BigDecimal budget = cash.subtract(params.commission());
                    BigDecimal quantity = budget.signum() > 0
                            ? budget.divide(price, params.wholeShares() ? 0 : MONEY_SCALE, RoundingMode.DOWN)
                            : BigDecimal.ZERO;
                    if (quantity.signum() <= 0) {
                        rejected++;
                    } else {
                        cash = cash.subtract(quantity.multiply(price)).subtract(params.commission());
                        shares = quantity;
                        totalCommission = totalCommission.add(params.commission());
                        trades.add(new Backtest.Trade(bar.date(), Backtest.Side.BUY, quantity, price,
                                params.commission()));
                    }
                } else {
                    BigDecimal price = bar.open().multiply(sellFactor).setScale(MONEY_SCALE, RoundingMode.HALF_EVEN);
                    BigDecimal proceeds = shares.multiply(price).subtract(params.commission());
                    if (proceeds.signum() < 0) {
                        rejected++;
                    } else {
                        cash = cash.add(proceeds);
                        totalCommission = totalCommission.add(params.commission());
                        trades.add(new Backtest.Trade(bar.date(), Backtest.Side.SELL, shares, price,
                                params.commission()));
                        shares = BigDecimal.ZERO;
                    }
                }
            }

            curve.add(new Backtest.EquityPoint(bar.date(),
                    cash.add(shares.multiply(bar.close())).setScale(MONEY_SCALE, RoundingMode.HALF_EVEN)));
            wanted = strategy.wantsPosition(bars, i);
        }

        return new Backtest.Result(List.copyOf(trades), List.copyOf(curve), curve.getLast().equity(),
                totalCommission, rejected);
    }

    private static void validate(List<Backtest.Bar> bars) {
        if (bars == null || bars.isEmpty()) {
            throw new IllegalArgumentException("No price bars to backtest");
        }
        LocalDate previous = null;
        for (Backtest.Bar bar : bars) {
            if (bar.date() == null || bar.open() == null || bar.close() == null
                    || bar.open().signum() <= 0 || bar.close().signum() <= 0) {
                throw new IllegalArgumentException("Every bar needs a date and a positive open and close");
            }
            if (previous != null && !bar.date().isAfter(previous)) {
                throw new IllegalArgumentException("Bars must be in ascending date order with unique dates");
            }
            previous = bar.date();
        }
    }
}