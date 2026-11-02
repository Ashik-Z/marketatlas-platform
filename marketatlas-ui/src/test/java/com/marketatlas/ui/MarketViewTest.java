package com.marketatlas.ui;

import com.marketatlas.ui.api.ApiClient;
import com.marketatlas.ui.api.ApiException;
import com.marketatlas.ui.api.AssetSummary;
import org.junit.jupiter.api.Test;

import com.marketatlas.ui.api.WatchlistEntry;
import com.marketatlas.ui.auth.UserSession;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class MarketViewTest {

    @Test
    void showsOneRowPerAsset() {
        ApiClient api = mock(ApiClient.class);
        when(api.listAssets()).thenReturn(List.of(
                new AssetSummary(1L, "Gold Spot", "COMMODITY", "XAU/USD", "USD", "troy ounce"),
                new AssetSummary(2L, "Apple", "EQUITY", "AAPL", "USD", null)));

        MarketView view = new MarketView(api, mock(UserSession.class));

        assertEquals(2, view.grid().getListDataView().getItemCount());
        assertEquals("", view.statusText());
    }

    @Test
    void emptyListShowsAMessage() {
        ApiClient api = mock(ApiClient.class);
        when(api.listAssets()).thenReturn(List.of());

        MarketView view = new MarketView(api, mock(UserSession.class));

        assertEquals(0, view.grid().getListDataView().getItemCount());
        assertEquals("No assets are registered yet.", view.statusText());
    }

    @Test
    void apiFailureShowsTheMessageAndNoRows() {
        ApiClient api = mock(ApiClient.class);
        when(api.listAssets()).thenThrow(
                new ApiException(ApiException.Kind.UNAVAILABLE, "The MarketAtlas API is not reachable right now.", null));

        MarketView view = new MarketView(api, mock(UserSession.class));

        assertEquals(0, view.grid().getListDataView().getItemCount());
        assertTrue(view.statusText().contains("not reachable"));
    }

    private static final AssetSummary GOLD = new AssetSummary(1L, "Gold Spot", "COMMODITY", "XAU/USD", "USD", "troy ounce");
    private static final AssetSummary APPLE = new AssetSummary(2L, "Apple", "EQUITY", "AAPL", "USD", null);

    private static UserSession loggedIn() {
        UserSession session = mock(UserSession.class);
        when(session.isLoggedIn()).thenReturn(true);
        when(session.authHeader()).thenReturn("Basic abc");
        return session;
    }

    private static WatchlistEntry entry(long assetId) {
        return new WatchlistEntry(assetId, "x", "COMMODITY", "USD", null, null, null, null, null, null, null);
    }

    @Test
    void loggedOutVisitorsGetNoWatchColumnAndNoWatchlistCall() {
        ApiClient api = mock(ApiClient.class);
        when(api.listAssets()).thenReturn(List.of(GOLD, APPLE));

        MarketView view = new MarketView(api, mock(UserSession.class));

        assertEquals(5, view.grid().getColumns().size());
        verify(api, never()).watchlist(org.mockito.ArgumentMatchers.anyString());
    }

    @Test
    void loggedInUsersGetAWatchColumnPreloadedFromTheirWatchlist() {
        ApiClient api = mock(ApiClient.class);
        when(api.listAssets()).thenReturn(List.of(GOLD, APPLE));
        when(api.watchlist("Basic abc")).thenReturn(List.of(entry(1L)));

        MarketView view = new MarketView(api, loggedIn());

        assertEquals(6, view.grid().getColumns().size());
        assertEquals(java.util.Set.of(1L), view.watchedIds());
    }

    @Test
    void togglingWatchesAnUnwatchedAssetAndUnwatchesAWatchedOne() {
        ApiClient api = mock(ApiClient.class);
        when(api.listAssets()).thenReturn(List.of(GOLD, APPLE));
        when(api.watchlist("Basic abc")).thenReturn(List.of(entry(1L)));
        MarketView view = new MarketView(api, loggedIn());

        view.toggle(APPLE);
        assertEquals(java.util.Set.of(1L, 2L), view.watchedIds());
        verify(api).watch("Basic abc", 2L);

        view.toggle(GOLD);
        assertEquals(java.util.Set.of(2L), view.watchedIds());
        verify(api).unwatch("Basic abc", 1L);
        assertEquals("", view.statusText());
    }

    @Test
    void aFullWatchlistShowsTheApisMessageAndKeepsTheStarOff() {
        ApiClient api = mock(ApiClient.class);
        when(api.listAssets()).thenReturn(List.of(GOLD, APPLE));
        when(api.watchlist("Basic abc")).thenReturn(List.of());
        org.mockito.Mockito.doThrow(new ApiException(ApiException.Kind.CONFLICT,
                        "Watchlist is full (50 assets maximum).", null))
                .when(api).watch("Basic abc", 2L);
        MarketView view = new MarketView(api, loggedIn());

        view.toggle(APPLE);

        assertTrue(view.watchedIds().isEmpty());
        assertEquals("Watchlist is full (50 assets maximum).", view.statusText());
    }

    @Test
    void ifTheWatchlistCannotBeLoadedTheMessageShowsAndThereIsNoWatchColumn() {
        ApiClient api = mock(ApiClient.class);
        when(api.listAssets()).thenReturn(List.of(GOLD));
        when(api.watchlist("Basic abc")).thenThrow(
                new ApiException(ApiException.Kind.UNAVAILABLE, "The MarketAtlas API is not reachable right now.", null));

        MarketView view = new MarketView(api, loggedIn());

        assertEquals(5, view.grid().getColumns().size());
        assertTrue(view.statusText().contains("not reachable"));
    }
}