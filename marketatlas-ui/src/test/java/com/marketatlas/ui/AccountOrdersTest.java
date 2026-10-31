package com.marketatlas.ui;

import com.marketatlas.ui.api.ApiClient;
import com.marketatlas.ui.api.ApiException;
import com.marketatlas.ui.api.AssetSummary;
import com.marketatlas.ui.api.Portfolio;
import com.marketatlas.ui.api.TradePage;
import com.marketatlas.ui.api.TradeRecord;
import com.marketatlas.ui.auth.UserSession;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AccountOrdersTest {

    private static final AssetSummary GOLD = new AssetSummary(7L, "Gold Spot", "COMMODITY", "XAU/USD", "USD", "troy ounce");
    private static final AssetSummary EURO = new AssetSummary(8L, "Euro Stock", "EQUITY", "EUS", "EUR", null);

    private static final TradeRecord FILL = new TradeRecord(11L, 3L, 7L, "BUY", new BigDecimal("0.05"),
            new BigDecimal("2200"), new BigDecimal("1"), LocalDate.of(2026, 10, 30), Instant.parse("2026-10-31T08:00:00Z"));

    private ApiClient api;
    private UserSession session;

    @BeforeEach
    void setUp() {
        api = mock(ApiClient.class);
        session = mock(UserSession.class);
        when(session.authHeader()).thenReturn("Basic abc");
        when(api.listAssets()).thenReturn(List.of(GOLD, EURO));
        when(api.portfolio(eq("Basic abc"), eq(3L))).thenReturn(new Portfolio(3L, "USD", new BigDecimal("1000"),
                new BigDecimal("900"), BigDecimal.ZERO, new BigDecimal("900"), new BigDecimal("-100"), BigDecimal.ZERO,
                BigDecimal.ZERO, true, List.of()));
        when(api.trades(eq("Basic abc"), eq(3L), anyInt(), anyInt())).thenReturn(
                new TradePage(List.of(FILL), 0, AccountView.PAGE_SIZE, 1, 1));
    }

    private AccountView openView() {
        AccountView view = new AccountView(api, session);
        view.open(3L);
        return view;
    }

    @Test
    void assetChoicesMatchTheAccountCurrency() {
        AccountView view = openView();

        assertEquals(1, view.assetBox().getListDataView().getItemCount());
    }

    @Test
    void validateOrderRejectsBadInput() {
        assertThrows(IllegalArgumentException.class, () -> AccountView.validateOrder(null, "BUY", BigDecimal.ONE));
        assertThrows(IllegalArgumentException.class, () -> AccountView.validateOrder(GOLD, null, BigDecimal.ONE));
        assertThrows(IllegalArgumentException.class, () -> AccountView.validateOrder(GOLD, "BUY", BigDecimal.ZERO));
        assertThrows(IllegalArgumentException.class, () -> AccountView.validateOrder(GOLD, "BUY", new BigDecimal("1.0000001")));
        assertThrows(IllegalArgumentException.class, () -> AccountView.validateOrder(GOLD, "BUY", new BigDecimal("100000000000000")));
        assertEquals(7L, AccountView.validateOrder(GOLD, "SELL", new BigDecimal("0.05")).assetId());
    }

    @Test
    void invalidOrderNeverCallsTheApi() {
        AccountView view = openView();

        view.placeOrder();

        assertEquals("Choose an asset.", view.orderStatusText());
        verify(api, never()).placeOrder(any(), anyLong(), anyString(), anyLong(), anyString(), any());
    }

    @Test
    void successfulOrderShowsTheFillAndRefreshes() {
        when(api.placeOrder(eq("Basic abc"), eq(3L), anyString(), eq(7L), eq("BUY"), eq(new BigDecimal("0.05"))))
                .thenReturn(FILL);
        AccountView view = openView();
        view.assetBox().setValue(GOLD);
        view.quantityField().setValue(new BigDecimal("0.05"));

        view.placeOrder();

        assertEquals("Filled: BUY 0.05 Gold Spot at 2200.00 (commission 1.00).", view.orderStatusText());
        verify(api, times(2)).portfolio("Basic abc", 3L);
        assertEquals(1, view.tradesGrid().getListDataView().getItemCount());
    }

    @Test
    void rejectedOrderShowsTheReasonAndNextAttemptUsesANewKey() {
        when(api.placeOrder(any(), anyLong(), anyString(), anyLong(), anyString(), any())).thenThrow(
                new ApiException(ApiException.Kind.CONFLICT, "Insufficient cash for this order", null));
        AccountView view = openView();
        view.assetBox().setValue(GOLD);
        view.quantityField().setValue(BigDecimal.ONE);

        view.placeOrder();
        view.placeOrder();

        assertEquals("Insufficient cash for this order", view.orderStatusText());
        ArgumentCaptor<String> keys = ArgumentCaptor.forClass(String.class);
        verify(api, times(2)).placeOrder(any(), anyLong(), keys.capture(), anyLong(), anyString(), any());
        assertNotEquals(keys.getAllValues().get(0), keys.getAllValues().get(1));
    }

    @Test
    void unreachableApiKeepsTheSameKeyForTheRetry() {
        when(api.placeOrder(any(), anyLong(), anyString(), anyLong(), anyString(), any())).thenThrow(
                new ApiException(ApiException.Kind.UNAVAILABLE, "The MarketAtlas API is not reachable right now.", null));
        AccountView view = openView();
        view.assetBox().setValue(GOLD);
        view.quantityField().setValue(BigDecimal.ONE);

        view.placeOrder();
        view.placeOrder();

        ArgumentCaptor<String> keys = ArgumentCaptor.forClass(String.class);
        verify(api, times(2)).placeOrder(any(), anyLong(), keys.capture(), anyLong(), anyString(), any());
        assertEquals(keys.getAllValues().get(0), keys.getAllValues().get(1));
    }

    @Test
    void changingTheFormStartsANewKey() {
        when(api.placeOrder(any(), anyLong(), anyString(), anyLong(), anyString(), any())).thenThrow(
                new ApiException(ApiException.Kind.UNAVAILABLE, "down", null));
        AccountView view = openView();
        view.assetBox().setValue(GOLD);
        view.quantityField().setValue(BigDecimal.ONE);

        view.placeOrder();
        view.quantityField().setValue(new BigDecimal("2"));
        view.placeOrder();

        ArgumentCaptor<String> keys = ArgumentCaptor.forClass(String.class);
        verify(api, times(2)).placeOrder(any(), anyLong(), keys.capture(), anyLong(), anyString(), any());
        assertNotEquals(keys.getAllValues().get(0), keys.getAllValues().get(1));
    }

    @Test
    void tradeHistoryErrorDoesNotHideThePortfolio() {
        when(api.trades(eq("Basic abc"), eq(3L), anyInt(), anyInt())).thenThrow(
                new ApiException(ApiException.Kind.UNAVAILABLE, "history down", null));

        AccountView view = openView();

        assertTrue(view.contentVisible());
        assertEquals("history down", view.tradesStatusText());
        assertEquals(0, view.tradesGrid().getListDataView().getItemCount());
    }

    @Test
    void pagerLabelShowsPageAndTotals() {
        AccountView view = openView();

        assertEquals("Page 1 of 1 (1 trades)", view.pageText());
    }
}