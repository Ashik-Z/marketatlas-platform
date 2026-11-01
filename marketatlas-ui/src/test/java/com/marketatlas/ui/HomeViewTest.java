package com.marketatlas.ui;

import com.marketatlas.ui.api.AccountSummary;
import com.marketatlas.ui.api.ApiClient;
import com.marketatlas.ui.api.ApiException;
import com.marketatlas.ui.api.Portfolio;
import com.marketatlas.ui.api.TradePage;
import com.marketatlas.ui.api.TradeRecord;
import com.marketatlas.ui.auth.UserSession;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class HomeViewTest {

    private ApiClient api;
    private UserSession session;

    @BeforeEach
    void setUp() {
        api = mock(ApiClient.class);
        session = mock(UserSession.class);
        when(session.authHeader()).thenReturn("Basic abc");
        when(session.username()).thenReturn("alice");
    }

    @Test
    void showsACardPerAccountAndTheLatestTrades() {
        when(api.listAccounts("Basic abc")).thenReturn(List.of(
                new AccountSummary(1L, "Main", "USD", new BigDecimal("10000"), new BigDecimal("9000"))));
        when(api.portfolio("Basic abc", 1L)).thenReturn(new Portfolio(1L, "USD", new BigDecimal("10000"),
                new BigDecimal("9000"), new BigDecimal("1058.96"), new BigDecimal("10058.96"), new BigDecimal("58.96"),
                BigDecimal.ZERO, new BigDecimal("58.96"), true, List.of()));
        when(api.trades(eq("Basic abc"), eq(1L), anyInt(), anyInt())).thenReturn(new TradePage(List.of(
                new TradeRecord(5L, 1L, 7L, "BUY", new BigDecimal("0.02"), new BigDecimal("4033.76"), BigDecimal.ONE,
                        LocalDate.of(2026, 10, 30), Instant.parse("2026-10-31T12:47:00Z"))), 0, 8, 1, 1));

        HomeView view = new HomeView(api, session);

        assertEquals(1, view.cardCount());
        assertEquals(1, view.tradesGrid().getListDataView().getItemCount());
        assertTrue(view.contentVisible());
        assertEquals("", view.statusText());
    }

    @Test
    void noAccountsInvitesTheUserToCreateOne() {
        when(api.listAccounts("Basic abc")).thenReturn(List.of());

        HomeView view = new HomeView(api, session);

        assertEquals(0, view.cardCount());
        assertEquals("You have no trading accounts yet. Create one under Accounts.", view.accountsNoteText());
        assertFalse(view.tradesGrid().isVisible());
    }

    @Test
    void apiDownShowsTheMessageAndHidesTheDashboard() {
        when(api.listAccounts("Basic abc")).thenThrow(
                new ApiException(ApiException.Kind.UNAVAILABLE, "The MarketAtlas API is not reachable right now.", null));

        HomeView view = new HomeView(api, session);

        assertEquals("The MarketAtlas API is not reachable right now.", view.statusText());
        assertFalse(view.contentVisible());
    }

    @Test
    void signedFiguresCarryASignAndAColorClass() {
        assertEquals("+58.96", HomeView.signed(new BigDecimal("58.964")));
        assertEquals("-2.00", HomeView.signed(new BigDecimal("-2")));
        assertEquals("0.00", HomeView.signed(new BigDecimal("0.001")));
        assertEquals("n/a", HomeView.signed(null));
        assertEquals("ma-gain", HomeView.pnlClass(new BigDecimal("1")));
        assertEquals("ma-loss", HomeView.pnlClass(new BigDecimal("-1")));
        assertEquals("ma-muted", HomeView.pnlClass(new BigDecimal("0.001")));
        assertEquals("\u25B2 ", HomeView.arrow(new BigDecimal("1")));
        assertEquals("\u25BC ", HomeView.arrow(new BigDecimal("-1")));
        assertEquals("", HomeView.arrow(BigDecimal.ZERO));
    }
}