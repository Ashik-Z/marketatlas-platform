package com.marketatlas.trading.service;

import com.marketatlas.trading.model.Trade;
import com.marketatlas.trading.model.TradeSide;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class CostBasisCalculatorTest {

    private static Trade trade(TradeSide side, String quantity, String price, String commission) {
        return new Trade(null, null, side, new BigDecimal(quantity), new BigDecimal(price),
                new BigDecimal(commission), LocalDate.of(2026, 10, 1), Instant.EPOCH, "k");
    }

    private static void assertNum(String expected, BigDecimal actual) {
        assertEquals(0, new BigDecimal(expected).compareTo(actual),
                "expected " + expected + " but was " + actual);
    }

    @Test
    void shouldReturnZerosWithNoTrades() {
        var result = CostBasisCalculator.calculate(List.of());

        assertNum("0", result.quantity());
        assertNum("0", result.costBasis());
        assertNum("0", result.realizedPnl());
        assertNum("0", result.averageCost());
    }

    @Test
    void shouldIncludeTheBuyCommissionInTheCost() {
        var result = CostBasisCalculator.calculate(List.of(trade(TradeSide.BUY, "2", "2000", "1")));

        assertNum("2", result.quantity());
        assertNum("4001", result.costBasis());
        assertNum("2000.5", result.averageCost());
        assertNum("0", result.realizedPnl());
    }

    @Test
    void shouldAverageTheCostAcrossBuys() {
        var result = CostBasisCalculator.calculate(List.of(
                trade(TradeSide.BUY, "2", "2000", "1"),
                trade(TradeSide.BUY, "1", "2300", "1")));

        assertNum("3", result.quantity());
        assertNum("6302", result.costBasis());
        assertNum("2100.666667", result.averageCost());
    }

    @Test
    void shouldRealizeProfitOnAPartialSaleAndKeepTheAverageCost() {
        var result = CostBasisCalculator.calculate(List.of(
                trade(TradeSide.BUY, "2", "2000", "1"),
                trade(TradeSide.SELL, "1", "2100", "1")));

        // cost removed 2000.5; proceeds 2100 - 1 = 2099; realized 98.5
        assertNum("1", result.quantity());
        assertNum("2000.5", result.costBasis());
        assertNum("2000.5", result.averageCost());
        assertNum("98.5", result.realizedPnl());
    }

    @Test
    void shouldRealizeALossAndClearTheCostWhenFullySold() {
        var result = CostBasisCalculator.calculate(List.of(
                trade(TradeSide.BUY, "2", "2000", "1"),
                trade(TradeSide.SELL, "2", "1900", "1")));

        // proceeds 3800 - 1 = 3799 against cost 4001
        assertNum("0", result.quantity());
        assertNum("0", result.costBasis());
        assertNum("-202", result.realizedPnl());
    }

    @Test
    void shouldNotLeaveRoundingCrumbsAcrossPartialSales() {
        // flat price: the only loss is the three commissions, so realized must be exactly -3
        var result = CostBasisCalculator.calculate(List.of(
                trade(TradeSide.BUY, "3", "100", "1"),
                trade(TradeSide.SELL, "1", "100", "1"),
                trade(TradeSide.SELL, "2", "100", "1")));

        assertNum("0", result.quantity());
        assertNum("0", result.costBasis());
        assertNum("-3", result.realizedPnl());
    }

    @Test
    void shouldHandleFractionalQuantities() {
        var result = CostBasisCalculator.calculate(List.of(trade(TradeSide.BUY, "0.5", "2000", "1")));

        assertNum("0.5", result.quantity());
        assertNum("1001", result.costBasis());
        assertNum("2002", result.averageCost());
    }
}