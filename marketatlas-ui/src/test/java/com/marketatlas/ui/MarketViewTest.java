package com.marketatlas.ui;

import com.marketatlas.ui.api.ApiClient;
import com.marketatlas.ui.api.ApiException;
import com.marketatlas.ui.api.AssetSummary;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class MarketViewTest {

    @Test
    void showsOneRowPerAsset() {
        ApiClient api = mock(ApiClient.class);
        when(api.listAssets()).thenReturn(List.of(
                new AssetSummary(1L, "Gold Spot", "COMMODITY", "XAU/USD", "USD", "troy ounce"),
                new AssetSummary(2L, "Apple", "EQUITY", "AAPL", "USD", null)));

        MarketView view = new MarketView(api);

        assertEquals(2, view.grid().getListDataView().getItemCount());
        assertEquals("", view.statusText());
    }

    @Test
    void emptyListShowsAMessage() {
        ApiClient api = mock(ApiClient.class);
        when(api.listAssets()).thenReturn(List.of());

        MarketView view = new MarketView(api);

        assertEquals(0, view.grid().getListDataView().getItemCount());
        assertEquals("No assets are registered yet.", view.statusText());
    }

    @Test
    void apiFailureShowsTheMessageAndNoRows() {
        ApiClient api = mock(ApiClient.class);
        when(api.listAssets()).thenThrow(
                new ApiException(ApiException.Kind.UNAVAILABLE, "The MarketAtlas API is not reachable right now.", null));

        MarketView view = new MarketView(api);

        assertEquals(0, view.grid().getListDataView().getItemCount());
        assertTrue(view.statusText().contains("not reachable"));
    }
}