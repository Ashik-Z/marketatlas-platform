package com.marketatlas.ui;

import com.marketatlas.ui.api.ApiClient;
import com.marketatlas.ui.api.ApiException;
import com.marketatlas.ui.api.DcaResult;
import com.marketatlas.ui.api.RealPrices;
import com.marketatlas.ui.auth.UserSession;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
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

class AssetDetailDcaTest {

    private static RealPrices prices() {
        return new RealPrices(7L, "Gold Spot", "USD", "CPIAUCNS", "CPI", "Source: FRED", LocalDate.of(2001, 3, 1),
                new BigDecimal("121"), LocalDate.of(2001, 3, 1), new BigDecimal("21.00"), new BigDecimal("0.00"),
                List.of(new RealPrices.Point(LocalDate.of(2001, 1, 15), new BigDecimal("50"), new BigDecimal("60.5"),
                        LocalDate.of(2001, 1, 1), false)));
    }

    private static DcaResult dca() {
        return new DcaResult(7L, "Gold Spot", "USD", "CPIAUCNS", "Source: FRED",
                LocalDate.of(2001, 3, 1), LocalDate.of(2001, 3, 1), new BigDecimal("100"), 2,
                new BigDecimal("200.00"), new BigDecimal("221.00"), new BigDecimal("3.65289256"),
                LocalDate.of(2001, 3, 10), new BigDecimal("60.5"), new BigDecimal("221.00"), new BigDecimal("221.00"),
                new BigDecimal("21.00"), new BigDecimal("0.00"), new BigDecimal("10.5000"), new BigDecimal("0.0000"),
                List.of(new DcaResult.Purchase(LocalDate.of(2001, 1, 15), new BigDecimal("50"), new BigDecimal("2"),
                                new BigDecimal("100"), new BigDecimal("121.00"), false),
                        new DcaResult.Purchase(LocalDate.of(2001, 3, 10), new BigDecimal("60.5"),
                                new BigDecimal("1.65289256"), new BigDecimal("100"), new BigDecimal("100.00"), true)));
    }

    private static AssetDetailView openView(ApiClient api) {
        AssetDetailView view = new AssetDetailView(api, mock(UserSession.class));
        view.open(7L);
        return view;
    }

    @Test
    void panelShowsOnlyWhenPricesLoad() {
        ApiClient api = mock(ApiClient.class);
        when(api.realPrices(eq(7L), any(LocalDate.class), any(LocalDate.class), isNull())).thenReturn(prices());
        when(api.realPrices(eq(9L), any(LocalDate.class), any(LocalDate.class), isNull())).thenThrow(
                new ApiException(ApiException.Kind.NOT_FOUND, "Asset not found: 9", null));

        AssetDetailView view = new AssetDetailView(api, mock(UserSession.class));
        assertFalse(view.dcaPanelVisible());
        view.open(7L);
        assertTrue(view.dcaPanelVisible());
        view.open(9L);
        assertFalse(view.dcaPanelVisible());
    }

    @Test
    void calculateSendsTheAmountAndShowsTheFourCards() {
        ApiClient api = mock(ApiClient.class);
        when(api.realPrices(eq(7L), any(LocalDate.class), any(LocalDate.class), isNull())).thenReturn(prices());
        when(api.dca(eq(7L), any(LocalDate.class), any(LocalDate.class), eq(new BigDecimal("250")))).thenReturn(dca());
        AssetDetailView view = openView(api);

        view.dcaAmountField().setValue(new BigDecimal("250"));
        view.calculateDca();

        assertEquals(List.of("Invested: 200.00", "Worth now: 221.00", "Nominal gain: 21.00", "Real gain: 0.00"),
                view.dcaSummary());
        assertEquals("", view.dcaStatusText());
    }

    @Test
    void badAmountNeverCallsTheApi() {
        ApiClient api = mock(ApiClient.class);
        when(api.realPrices(eq(7L), any(LocalDate.class), any(LocalDate.class), isNull())).thenReturn(prices());
        AssetDetailView view = openView(api);

        view.dcaAmountField().setValue(BigDecimal.ZERO);
        view.calculateDca();
        assertEquals("Enter an amount greater than zero.", view.dcaStatusText());

        view.dcaAmountField().clear();
        view.calculateDca();
        assertEquals("Enter an amount greater than zero.", view.dcaStatusText());
        assertTrue(view.dcaSummary().isEmpty());
        verify(api, never()).dca(any(), any(), any(), any());
    }

    @Test
    void apiErrorShowsTheMessageAndNoCards() {
        ApiClient api = mock(ApiClient.class);
        when(api.realPrices(eq(7L), any(LocalDate.class), any(LocalDate.class), isNull())).thenReturn(prices());
        when(api.dca(eq(7L), any(LocalDate.class), any(LocalDate.class), any(BigDecimal.class))).thenThrow(
                new ApiException(ApiException.Kind.INVALID_INPUT,
                        "No usable price bars with CPI data in this date range", null));
        AssetDetailView view = openView(api);

        view.calculateDca();

        assertEquals("No usable price bars with CPI data in this date range", view.dcaStatusText());
        assertTrue(view.dcaSummary().isEmpty());
    }
}
