package com.marketatlas.ui;

import com.marketatlas.ui.api.ApiClient;
import com.marketatlas.ui.api.ApiException;
import com.marketatlas.ui.api.Portfolio;
import com.marketatlas.ui.auth.UserSession;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class AccountViewTest {

    private static Portfolio.Position priced(boolean stale) {
        return new Portfolio.Position(7L, "Gold Spot", new BigDecimal("0.050000"), new BigDecimal("2000"),
                new BigDecimal("100"), new BigDecimal("2200"), LocalDate.of(2026, 10, 1), new BigDecimal("110"),
                new BigDecimal("10"), stale);
    }

    private static Portfolio.Position unpriced() {
        return new Portfolio.Position(1L, "Apple Inc.", new BigDecimal("2"), new BigDecimal("200"),
                new BigDecimal("400"), null, null, null, null, true);
    }

    private static Portfolio portfolio(boolean complete, List<Portfolio.Position> positions) {
        return new Portfolio(3L, "USD", new BigDecimal("1000"), new BigDecimal("900"), new BigDecimal("110"),
                new BigDecimal("1010"), new BigDecimal("10"), new BigDecimal("2"), new BigDecimal("8"),
                complete, positions);
    }

    @Test
    void showsSummaryAndPositions() {
        ApiClient api = mock(ApiClient.class);
        UserSession session = mock(UserSession.class);
        when(session.authHeader()).thenReturn("Basic abc");
        when(api.portfolio(eq("Basic abc"), eq(3L))).thenReturn(portfolio(true, List.of(priced(false))));
        AccountView view = new AccountView(api, session);

        view.open(3L);

        assertTrue(view.contentVisible());
        assertEquals(6, view.summaryGrid().getListDataView().getItemCount());
        assertEquals(1, view.positionsGrid().getListDataView().getItemCount());
        assertEquals("", view.statusText());
    }

    @Test
    void summaryRowsFormatAmounts() {
        List<AccountView.Row> rows = AccountView.summaryRows(portfolio(true, List.of()));

        assertEquals("900.00", rows.get(0).value());
        assertEquals("1010.00", rows.get(2).value());
        assertEquals("Realized P&L", rows.get(4).label());
    }

    @Test
    void priceTextFlagsMissingAndStalePrices() {
        assertEquals("no price", AccountView.priceText(unpriced()));
        assertEquals("2200.00 (stale, 2026-10-01)", AccountView.priceText(priced(true)));
        assertEquals("2200.00", AccountView.priceText(priced(false)));
    }

    @Test
    void notFoundShowsTheMessageAndHidesTheContent() {
        ApiClient api = mock(ApiClient.class);
        UserSession session = mock(UserSession.class);
        when(session.authHeader()).thenReturn("Basic abc");
        when(api.portfolio(eq("Basic abc"), eq(9L))).thenThrow(
                new ApiException(ApiException.Kind.NOT_FOUND, "Account not found: 9", null));
        AccountView view = new AccountView(api, session);

        view.open(9L);

        assertEquals("Account not found: 9", view.statusText());
        assertFalse(view.contentVisible());
    }
}