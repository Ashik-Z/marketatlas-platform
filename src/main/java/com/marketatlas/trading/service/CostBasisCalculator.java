package com.marketatlas.trading.service;

import com.marketatlas.trading.model.Trade;
import com.marketatlas.trading.model.TradeSide;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

/**
 * Average-cost accounting for one asset in one account. Trades must be in execution order.
 * Buy commissions are part of the cost; sell commissions reduce the proceeds.
 */
public final class CostBasisCalculator {

    private static final int SCALE = 6;

    private CostBasisCalculator() {
    }

    public record Result(BigDecimal quantity, BigDecimal costBasis, BigDecimal realizedPnl) {

        public BigDecimal averageCost() {
            return quantity.signum() == 0
                    ? BigDecimal.ZERO.setScale(SCALE)
                    : costBasis.divide(quantity, SCALE, RoundingMode.HALF_EVEN);
        }
    }

    public static Result calculate(List<Trade> tradesInOrder) {
        BigDecimal quantity = BigDecimal.ZERO.setScale(SCALE);
        BigDecimal cost = BigDecimal.ZERO.setScale(SCALE);
        BigDecimal realized = BigDecimal.ZERO.setScale(SCALE);

        for (Trade trade : tradesInOrder) {
            BigDecimal gross = trade.getPrice().multiply(trade.getQuantity())
                    .setScale(SCALE, RoundingMode.HALF_EVEN);

            if (trade.getSide() == TradeSide.BUY) {
                cost = cost.add(gross).add(trade.getCommission());
                quantity = quantity.add(trade.getQuantity());
            } else {
                BigDecimal costRemoved = trade.getQuantity().compareTo(quantity) == 0
                        ? cost
                        : cost.multiply(trade.getQuantity())
                        .divide(quantity, SCALE, RoundingMode.HALF_EVEN);
                realized = realized.add(gross.subtract(trade.getCommission()).subtract(costRemoved));
                cost = cost.subtract(costRemoved);
                quantity = quantity.subtract(trade.getQuantity());
            }
        }
        return new Result(quantity, cost, realized);
    }
}