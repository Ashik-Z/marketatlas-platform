package com.marketatlas.backtest;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.function.IntPredicate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BacktestEngineTest {

    private static Backtest.Bar bar(int day, String open, String close) {
        return new Backtest.Bar(LocalDate.of(2026, 1, day), new BigDecimal(open), new BigDecimal(close));
    }

    private static Backtest.Params params(String cash, String commission, int bps, boolean whole) {
        return new Backtest.Params(new BigDecimal(cash), new BigDecimal(commission), bps, whole);
    }

    private static Strategy wantWhile(IntPredicate condition) {
        return new Strategy() {
            @Override
            public boolean wantsPosition(List<Backtest.Bar> bars, int index) {
                return condition.test(index);
            }

            @Override
            public String name() {
                return "TEST";
            }
        };
    }

    private static String plain(BigDecimal value) {
        return value.stripTrailingZeros().toPlainString();
    }

    private static List<String> equities(Backtest.Result result) {
        return result.equityCurve().stream().map(p -> plain(p.equity())).toList();
    }

    @Test
    void buyAndHoldFillsAtTheSecondBarsOpenWithWholeShares() {
        // d1 decision -> d2 open 10: budget 1000-1=999, floor(99.9)=99 shares, cost 990, cash 1000-990-1=9
        // closes 10,12,15 -> equity 1000, 9+99*12=1197, 9+99*15=1494
        var result = BacktestEngine.run(
                List.of(bar(1, "10", "10"), bar(2, "10", "12"), bar(3, "12", "15")),
                Strategies.buyAndHold(), params("1000", "1", 0, true));

        assertEquals(1, result.trades().size());
        var buy = result.trades().get(0);
        assertEquals(LocalDate.of(2026, 1, 2), buy.date());
        assertEquals("99", plain(buy.quantity()));
        assertEquals("10", plain(buy.price()));
        assertEquals(List.of("1000", "1197", "1494"), equities(result));
        assertEquals("1494", plain(result.finalEquity()));
        assertEquals("1", plain(result.totalCommission()));
        assertEquals(0, result.rejectedOrders());
    }

    @Test
    void fractionalAssetsBuyWithAllRemainingCash() {
        // 999 / 10 = 99.9 units, cost 999, cash 0 -> equity 99.9*12=1198.8, 99.9*15=1498.5
        var result = BacktestEngine.run(
                List.of(bar(1, "10", "10"), bar(2, "10", "12"), bar(3, "12", "15")),
                Strategies.buyAndHold(), params("1000", "1", 0, false));

        assertEquals("99.9", plain(result.trades().get(0).quantity()));
        assertEquals(List.of("1000", "1198.8", "1498.5"), equities(result));
    }

    @Test
    void decisionFillsAtTheNextOpenNeverAtADayItWasMadeFrom() {
        // Wants a position only after d2's close (close 20). It must fill at d3's open 50, not 20 or 60.
        // budget 999, floor(999/50)=19, cost 950, cash 49 -> equity 1000,1000,49+950=999,49+19*60=1189
        var result = BacktestEngine.run(
                List.of(bar(1, "10", "10"), bar(2, "10", "20"), bar(3, "50", "50"), bar(4, "60", "60")),
                wantWhile(i -> i >= 1), params("1000", "1", 0, true));

        var buy = result.trades().get(0);
        assertEquals(LocalDate.of(2026, 1, 3), buy.date());
        assertEquals("50", plain(buy.price()));
        assertEquals("19", plain(buy.quantity()));
        assertEquals(List.of("1000", "1000", "999", "1189"), equities(result));
    }

    @Test
    void roundTripAppliesSlippageAndCommissionOnBothSides() {
        // 1% slippage, commission 2. Buy d2: price 100*1.01=101, budget 998, 9 shares, cost 909, cash 89.
        // Sell d4: price 130*0.99=128.7, proceeds 9*128.7-2=1156.3, cash 1245.3. Commission total 4.
        var result = BacktestEngine.run(
                List.of(bar(1, "100", "100"), bar(2, "100", "110"), bar(3, "110", "120"), bar(4, "130", "130")),
                wantWhile(i -> i < 2), params("1000", "2", 100, true));

        assertEquals(2, result.trades().size());
        assertEquals("101", plain(result.trades().get(0).price()));
        assertEquals("128.7", plain(result.trades().get(1).price()));
        assertEquals(Backtest.Side.SELL, result.trades().get(1).side());
        assertEquals(List.of("1000", "1079", "1169", "1245.3"), equities(result));
        assertEquals("1245.3", plain(result.finalEquity()));
        assertEquals("4", plain(result.totalCommission()));
    }

    @Test
    void cashBelowTheCommissionIsARejectedOrderNotANegativeBalance() {
        // cash 5, commission 10: both attempted buys (d2, d3) are rejected; equity stays 5.
        var result = BacktestEngine.run(
                List.of(bar(1, "10", "10"), bar(2, "10", "10"), bar(3, "10", "10")),
                Strategies.buyAndHold(), params("5", "10", 0, true));

        assertTrue(result.trades().isEmpty());
        assertEquals(2, result.rejectedOrders());
        assertEquals(List.of("5", "5", "5"), equities(result));
    }

    @Test
    void saleThatWouldNotCoverTheCommissionIsRejectedAndThePositionStays() {
        // cash 16, commission 5: buy d2 at 10 -> budget 11, 1 share, cash 1. Sell attempt d4 at 4: 4-5 < 0 -> rejected.
        // equity 16, 1+10=11, 11, 1+4=5
        var result = BacktestEngine.run(
                List.of(bar(1, "10", "10"), bar(2, "10", "10"), bar(3, "10", "10"), bar(4, "4", "4")),
                wantWhile(i -> i < 2), params("16", "5", 0, true));

        assertEquals(1, result.trades().size());
        assertEquals(1, result.rejectedOrders());
        assertEquals(List.of("16", "11", "11", "5"), equities(result));
    }

    @Test
    void aSingleBarCannotTradeBecauseThereIsNoNextOpen() {
        var result = BacktestEngine.run(List.of(bar(1, "10", "10")), Strategies.buyAndHold(),
                params("1000", "1", 0, true));

        assertTrue(result.trades().isEmpty());
        assertEquals("1000", plain(result.finalEquity()));
    }

    @Test
    void invalidBarsAreRejected() {
        var p = params("1000", "1", 0, true);
        var s = Strategies.buyAndHold();

        assertThrows(IllegalArgumentException.class, () -> BacktestEngine.run(List.of(), s, p));
        assertThrows(IllegalArgumentException.class,
                () -> BacktestEngine.run(List.of(bar(2, "10", "10"), bar(1, "10", "10")), s, p));
        assertThrows(IllegalArgumentException.class,
                () -> BacktestEngine.run(List.of(bar(1, "0", "10")), s, p));
    }
}