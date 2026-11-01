package com.marketatlas.ui;

import com.marketatlas.ui.api.AccountSummary;
import com.marketatlas.ui.api.ApiClient;
import com.marketatlas.ui.api.ApiException;
import com.marketatlas.ui.api.AssetSummary;
import com.marketatlas.ui.api.Portfolio;
import com.marketatlas.ui.api.TradePage;
import com.marketatlas.ui.api.TradeRecord;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class DashboardModelTest {

    private static final String AUTH = "Basic abc";
    private ApiClient api;

    @BeforeEach
    void setUp() {
        api = mock(ApiClient.class);
        when(api.listAssets()).thenReturn(List.of(
                new AssetSummary(7L, "Gold Spot", "COMMODITY", "XAU/USD", "USD", "troy ounce")));
    }

    private static AccountSummary account(long id, String name) {
        return new AccountSummary(id, name, "USD", new BigDecimal("10000"), new BigDecimal("9000"));
    }

    private static Portfolio portfolio(long accountId, String starting, String pnl) {
        return new Portfolio(accountId, "USD", new BigDecimal(starting), new BigDecimal("900"), new BigDecimal("110"),
                new BigDecimal("1010"), new BigDecimal(pnl), new BigDecimal("2"), new BigDecimal("8"), true, List.of());
    }

    private static TradeRecord trade(long id, long accountId, String when) {
        return new TradeRecord(id, accountId, 7L, "BUY", new BigDecimal("0.05"), new BigDecimal("2200"),
                BigDecimal.ONE, LocalDate.of(2026, 10, 30), Instant.parse(when));
    }

    @Test
    void buildsCardsAndMergesTradesNewestFirst() {
        when(api.listAccounts(AUTH)).thenReturn(List.of(account(1, "Main"), account(3, "Test")));
        when(api.portfolio(AUTH, 1L)).thenReturn(portfolio(1, "10000", "58.96"));
        when(api.portfolio(AUTH, 3L)).thenReturn(portfolio(3, "100", "-2"));
        when(api.trades(AUTH, 1L, 0, DashboardModel.MAX_TRADES)).thenReturn(new TradePage(
                List.of(trade(1, 1, "2026-10-30T08:00:00Z"), trade(2, 1, "2026-10-31T08:00:00Z")), 0, 8, 2, 1));
        when(api.trades(AUTH, 3L, 0, DashboardModel.MAX_TRADES)).thenReturn(new TradePage(
                List.of(trade(3, 3, "2026-10-31T12:00:00Z")), 0, 8, 1, 1));

        DashboardModel.Model m = DashboardModel.load(api, AUTH);

        assertEquals(2, m.cards().size());
        assertEquals(new BigDecimal("0.59"), m.cards().get(0).returnPct());
        assertEquals(new BigDecimal("-2.00"), m.cards().get(1).returnPct());
        assertEquals(List.of("Test", "Main", "Main"), m.trades().stream().map(DashboardModel.RecentTrade::accountName).toList());
        assertEquals("Gold Spot", m.trades().get(0).assetName());
        assertEquals(2, m.totalAccounts());
    }

    @Test
    void showsOnlyTheFirstSixAccountsAndTheEightNewestTrades() {
        List<AccountSummary> accounts = new ArrayList<>();
        for (int i = 1; i <= 8; i++) {
            accounts.add(account(i, "A" + i));
        }
        when(api.listAccounts(AUTH)).thenReturn(accounts);
        when(api.portfolio(org.mockito.ArgumentMatchers.eq(AUTH), anyLong())).thenReturn(portfolio(1, "10000", "1"));
        when(api.trades(org.mockito.ArgumentMatchers.eq(AUTH), anyLong(), anyInt(), anyInt())).thenAnswer(inv -> {
            long id = inv.getArgument(1);
            return new TradePage(List.of(trade(id * 10, id, Instant.parse("2026-10-20T00:00:00Z").plusSeconds(id * 3600).toString()),
                    trade(id * 10 + 1, id, Instant.parse("2026-10-20T00:00:00Z").plusSeconds(id * 3600 + 60).toString())), 0, 8, 2, 1);
        });

        DashboardModel.Model m = DashboardModel.load(api, AUTH);

        assertEquals(6, m.cards().size());
        assertEquals(8, m.totalAccounts());
        assertEquals(8, m.trades().size());
    }

    @Test
    void aFailedPortfolioMakesAnUnavailableCardWithoutBreakingThePage() {
        when(api.listAccounts(AUTH)).thenReturn(List.of(account(1, "Main")));
        when(api.portfolio(AUTH, 1L)).thenThrow(new ApiException(ApiException.Kind.UNAVAILABLE, "down", null));
        when(api.trades(AUTH, 1L, 0, DashboardModel.MAX_TRADES)).thenThrow(
                new ApiException(ApiException.Kind.UNAVAILABLE, "down", null));

        DashboardModel.Model m = DashboardModel.load(api, AUTH);

        assertFalse(m.cards().get(0).available());
        assertNull(m.cards().get(0).profitLoss());
        assertTrue(m.trades().isEmpty());
    }

    @Test
    void returnIsUnknownWhenTheStartingBalanceIsZero() {
        DashboardModel.AccountCard c = DashboardModel.card(account(1, "Main"), portfolio(1, "0", "5"));

        assertNull(c.returnPct());
        assertTrue(c.available());
    }

    @Test
    void accountListFailurePropagates() {
        when(api.listAccounts(AUTH)).thenThrow(new ApiException(ApiException.Kind.UNAVAILABLE, "API down", null));

        assertThrows(ApiException.class, () -> DashboardModel.load(api, AUTH));
    }
}