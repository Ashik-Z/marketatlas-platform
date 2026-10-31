package com.marketatlas.ui;

import com.marketatlas.ui.api.ApiClient;
import com.marketatlas.ui.api.ApiException;
import com.marketatlas.ui.api.AssetSummary;
import com.marketatlas.ui.api.BacktestParams;
import com.marketatlas.ui.api.BacktestResult;
import com.marketatlas.ui.auth.UserSession;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class BacktestViewTest {

    private static final AssetSummary GOLD = new AssetSummary(7L, "Gold Spot", "COMMODITY", "XAU/USD", "USD", "troy ounce");
    private static final LocalDate FROM = LocalDate.of(2024, 1, 1);
    private static final LocalDate TO = LocalDate.of(2024, 6, 30);

    private static BacktestResult.Metrics metrics(String total) {
        return new BacktestResult.Metrics(new BigDecimal(total), new BigDecimal("20"), new BigDecimal("-5"),
                new BigDecimal("14"), 3, new BigDecimal("3"), new BigDecimal("60"), 0);
    }

    private static BacktestResult result(boolean withRealTerms) {
        BacktestResult.RealTerms rt = withRealTerms
                ? new BacktestResult.RealTerms("2024-01", new BigDecimal("1.2"), new BigDecimal("11100.5"),
                        new BigDecimal("11"), new BigDecimal("11360"), new BigDecimal("13.6"), false)
                : null;
        List<BacktestResult.CurvePoint> curve = List.of(new BacktestResult.CurvePoint(FROM, new BigDecimal("10000")));
        return new BacktestResult(7L, "Gold Spot", "USD", "SMA_CROSSOVER", FROM, TO, 125, 0, new BigDecimal("10000"),
                metrics("12.5"), metrics("15"), List.of(), curve, curve, rt, withRealTerms ? null : "Real terms need USD.");
    }

    @Test
    void buildParamsRejectsBadInput() {
        assertThrows(IllegalArgumentException.class, () ->
                BacktestView.buildParams(null, FROM, TO, "SMA_CROSSOVER", 20, 50, BigDecimal.TEN, 0));
        assertThrows(IllegalArgumentException.class, () ->
                BacktestView.buildParams(GOLD, TO, FROM, "SMA_CROSSOVER", 20, 50, BigDecimal.TEN, 0));
        assertThrows(IllegalArgumentException.class, () ->
                BacktestView.buildParams(GOLD, FROM, TO, "SMA_CROSSOVER", 50, 20, BigDecimal.TEN, 0));
        assertThrows(IllegalArgumentException.class, () ->
                BacktestView.buildParams(GOLD, FROM, TO, "SMA_CROSSOVER", 20, 401, BigDecimal.TEN, 0));
        assertThrows(IllegalArgumentException.class, () ->
                BacktestView.buildParams(GOLD, FROM, TO, "BUY_AND_HOLD", null, null, BigDecimal.ZERO, 0));
        assertThrows(IllegalArgumentException.class, () ->
                BacktestView.buildParams(GOLD, FROM, TO, "BUY_AND_HOLD", null, null, BigDecimal.TEN, 1001));
    }

    @Test
    void buyAndHoldIgnoresTheWindows() {
        BacktestParams p = BacktestView.buildParams(GOLD, FROM, TO, "BUY_AND_HOLD", 99, 5, BigDecimal.TEN, null);

        assertNull(p.shortWindow());
        assertNull(p.longWindow());
        assertEquals(0, p.slippageBps());
        assertTrue(p.includeEquityCurve());
    }

    @Test
    void rowsIncludeRealTermsOnlyWhenPresent() {
        List<BacktestView.Row> with = BacktestView.rows(result(true));
        List<BacktestView.Row> without = BacktestView.rows(result(false));

        assertEquals(9, with.size());
        assertEquals(7, without.size());
        assertEquals("12.50%", with.get(0).strategy());
        assertEquals("15.00%", with.get(0).benchmark());
        assertEquals("Real total return (2024-01 dollars)", with.get(7).label());
    }

    @Test
    void successfulRunShowsTheResult() {
        ApiClient api = mock(ApiClient.class);
        UserSession session = mock(UserSession.class);
        when(api.listAssets()).thenReturn(List.of(GOLD));
        when(session.authHeader()).thenReturn("Basic abc");
        when(api.backtest(eq(7L), eq("Basic abc"), any(BacktestParams.class))).thenReturn(result(true));
        BacktestView view = new BacktestView(api, session);
        view.assetBox().setValue(GOLD);

        view.run();

        assertTrue(view.resultVisible());
        assertEquals("", view.statusText());
        assertEquals(9, view.metricsGrid().getListDataView().getItemCount());
    }

    @Test
    void rateLimitShowsTheWaitTimeAndHidesResults() {
        ApiClient api = mock(ApiClient.class);
        UserSession session = mock(UserSession.class);
        when(api.listAssets()).thenReturn(List.of(GOLD));
        when(session.authHeader()).thenReturn("Basic abc");
        when(api.backtest(eq(7L), eq("Basic abc"), any(BacktestParams.class))).thenThrow(
                new ApiException(ApiException.Kind.RATE_LIMITED, "Too many attempts.", 30L));
        BacktestView view = new BacktestView(api, session);
        view.assetBox().setValue(GOLD);

        view.run();

        assertEquals("Too many attempts. Try again in 30 seconds.", view.statusText());
        assertFalse(view.resultVisible());
    }

    @Test
    void runWithoutAnAssetAsksForOne() {
        ApiClient api = mock(ApiClient.class);
        when(api.listAssets()).thenReturn(List.of(GOLD));
        BacktestView view = new BacktestView(api, mock(UserSession.class));

        view.run();

        assertEquals("Choose an asset.", view.statusText());
        assertFalse(view.resultVisible());
    }
}