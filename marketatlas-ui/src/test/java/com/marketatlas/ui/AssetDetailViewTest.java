package com.marketatlas.ui;

import com.marketatlas.ui.api.ApiClient;
import com.marketatlas.ui.api.ApiException;
import com.marketatlas.ui.api.WatchlistEntry;
import com.marketatlas.ui.auth.UserSession;
import com.marketatlas.ui.api.ApiException;
import com.marketatlas.ui.api.RealPrices;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AssetDetailViewTest {

    private static RealPrices sample() {
        return new RealPrices(7L, "Gold Spot", "USD", "CPIAUCNS", "CPI", "Source: FRED", LocalDate.of(2024, 1, 1),
                new BigDecimal("308.417"), LocalDate.of(2024, 2, 1), new BigDecimal("1.50"), new BigDecimal("1.10"),
                List.of(new RealPrices.Point(LocalDate.of(2024, 1, 2), new BigDecimal("2000.5"), new BigDecimal("2000.5"),
                        LocalDate.of(2024, 1, 1), false)));
    }

    @Test
    void successShowsSummaryTitleAndChart() {
        ApiClient api = mock(ApiClient.class);
        when(api.realPrices(eq(7L), any(LocalDate.class), any(LocalDate.class), isNull())).thenReturn(sample());

        AssetDetailView view = new AssetDetailView(api, org.mockito.Mockito.mock(UserSession.class));
        view.open(7L);

        assertEquals("Gold Spot (USD)", view.titleText());
        assertEquals("Nominal return 1.50%, real return 1.10% (in 2024-01 dollars).", view.summaryText());
        assertTrue(view.chartVisible());
        assertEquals("", view.statusText());
    }

    @Test
    void apiErrorShowsTheMessageAndHidesTheChart() {
        ApiClient api = mock(ApiClient.class);
        when(api.realPrices(eq(9L), any(LocalDate.class), any(LocalDate.class), isNull())).thenThrow(
                new ApiException(ApiException.Kind.NOT_FOUND, "Asset not found: 9", null));

        AssetDetailView view = new AssetDetailView(api, org.mockito.Mockito.mock(UserSession.class));
        view.open(9L);

        assertEquals("Asset not found: 9", view.statusText());
        assertEquals("", view.summaryText());
        assertFalse(view.chartVisible());
    }

    @Test
    void startAfterEndIsRejectedWithoutCallingTheApi() {
        ApiClient api = mock(ApiClient.class);
        AssetDetailView view = new AssetDetailView(api, org.mockito.Mockito.mock(UserSession.class));
        view.startPicker().setValue(LocalDate.of(2025, 1, 2));
        view.endPicker().setValue(LocalDate.of(2025, 1, 1));

        view.open(7L);

        assertEquals("The start date must be on or before the end date.", view.statusText());
        assertFalse(view.chartVisible());
        verify(api, never()).realPrices(any(), any(), any(), any(YearMonth.class));
        verify(api, never()).realPrices(any(), any(), any(), isNull());
    }

    private static UserSession loggedIn() {
        UserSession session = org.mockito.Mockito.mock(UserSession.class);
        org.mockito.Mockito.when(session.isLoggedIn()).thenReturn(true);
        org.mockito.Mockito.when(session.authHeader()).thenReturn("Basic abc");
        return session;
    }

    private static ApiClient apiWithFailingPrices() {
        ApiClient api = org.mockito.Mockito.mock(ApiClient.class);
        org.mockito.Mockito.when(api.realPrices(org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any(),
                        org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any()))
                .thenThrow(new ApiException(ApiException.Kind.NOT_FOUND, "Not found.", null));
        return api;
    }

    private static WatchlistEntry entry(long assetId) {
        return new WatchlistEntry(assetId, "x", "COMMODITY", "USD", null, null, null, null, null, null, null);
    }

    @org.junit.jupiter.api.Test
    void loggedOutVisitorsDoNotSeeTheWatchButton() {
        AssetDetailView view = new AssetDetailView(apiWithFailingPrices(), org.mockito.Mockito.mock(UserSession.class));

        view.open(5L);

        org.junit.jupiter.api.Assertions.assertFalse(view.watchVisible());
    }

    @org.junit.jupiter.api.Test
    void watchButtonShowsWatchingWhenTheAssetIsAlreadyWatched() {
        ApiClient api = apiWithFailingPrices();
        org.mockito.Mockito.when(api.watchlist("Basic abc")).thenReturn(java.util.List.of(entry(5L)));
        AssetDetailView view = new AssetDetailView(api, loggedIn());

        view.open(5L);

        org.junit.jupiter.api.Assertions.assertTrue(view.watchVisible());
        org.junit.jupiter.api.Assertions.assertEquals("\u2605 Watching", view.watchLabel());
    }

    @org.junit.jupiter.api.Test
    void togglingWatchesThenUnwatches() {
        ApiClient api = apiWithFailingPrices();
        org.mockito.Mockito.when(api.watchlist("Basic abc")).thenReturn(java.util.List.of());
        AssetDetailView view = new AssetDetailView(api, loggedIn());
        view.open(5L);
        org.junit.jupiter.api.Assertions.assertEquals("\u2606 Watch", view.watchLabel());

        view.toggleWatch();
        org.junit.jupiter.api.Assertions.assertEquals("\u2605 Watching", view.watchLabel());
        org.mockito.Mockito.verify(api).watch("Basic abc", 5L);

        view.toggleWatch();
        org.junit.jupiter.api.Assertions.assertEquals("\u2606 Watch", view.watchLabel());
        org.mockito.Mockito.verify(api).unwatch("Basic abc", 5L);
    }

    @org.junit.jupiter.api.Test
    void aFullWatchlistLeavesTheButtonUnwatched() {
        ApiClient api = apiWithFailingPrices();
        org.mockito.Mockito.when(api.watchlist("Basic abc")).thenReturn(java.util.List.of());
        org.mockito.Mockito.doThrow(new ApiException(ApiException.Kind.CONFLICT, "Watchlist is full (50 assets maximum).", null))
                .when(api).watch("Basic abc", 5L);
        AssetDetailView view = new AssetDetailView(api, loggedIn());
        view.open(5L);

        view.toggleWatch();

        org.junit.jupiter.api.Assertions.assertEquals("\u2606 Watch", view.watchLabel());
    }

    @org.junit.jupiter.api.Test
    void ifTheWatchlistCannotBeLoadedTheButtonHides() {
        ApiClient api = apiWithFailingPrices();
        org.mockito.Mockito.when(api.watchlist("Basic abc")).thenThrow(
                new ApiException(ApiException.Kind.UNAVAILABLE, "down", null));
        AssetDetailView view = new AssetDetailView(api, loggedIn());

        view.open(5L);

        org.junit.jupiter.api.Assertions.assertFalse(view.watchVisible());
    }
}