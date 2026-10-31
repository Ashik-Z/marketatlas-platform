package com.marketatlas.ui;

import com.marketatlas.ui.api.ApiClient;
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

        AssetDetailView view = new AssetDetailView(api);
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

        AssetDetailView view = new AssetDetailView(api);
        view.open(9L);

        assertEquals("Asset not found: 9", view.statusText());
        assertEquals("", view.summaryText());
        assertFalse(view.chartVisible());
    }

    @Test
    void startAfterEndIsRejectedWithoutCallingTheApi() {
        ApiClient api = mock(ApiClient.class);
        AssetDetailView view = new AssetDetailView(api);
        view.startPicker().setValue(LocalDate.of(2025, 1, 2));
        view.endPicker().setValue(LocalDate.of(2025, 1, 1));

        view.open(7L);

        assertEquals("The start date must be on or before the end date.", view.statusText());
        assertFalse(view.chartVisible());
        verify(api, never()).realPrices(any(), any(), any(), any(YearMonth.class));
        verify(api, never()).realPrices(any(), any(), any(), isNull());
    }
}