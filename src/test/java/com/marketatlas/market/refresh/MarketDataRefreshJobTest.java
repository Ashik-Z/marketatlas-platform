package com.marketatlas.market.refresh;

import com.marketatlas.common.exception.MarketDataProviderException;
import com.marketatlas.market.service.AssetService;
import com.marketatlas.market.service.MarketDataService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MarketDataRefreshJobTest {

    private static final LocalDate TODAY = LocalDate.of(2026, 10, 24);
    private static final LocalDate WINDOW_START = LocalDate.of(2026, 10, 17);

    @Mock
    private AssetService assetService;

    @Mock
    private MarketDataService marketDataService;

    private final Clock clock =
            Clock.fixed(Instant.parse("2026-10-24T12:00:00Z"), ZoneOffset.UTC);

    private MarketDataRefreshJob job(Duration delay) {
        return new MarketDataRefreshJob(
                assetService,
                marketDataService,
                new RefreshProperties(true, "0 30 22 * * MON-FRI", 7, delay),
                clock);
    }

    @Test
    void shouldRefreshEveryAssetOverTheLookbackWindow() {
        when(assetService.findAllProviderSymbols()).thenReturn(List.of("AAPL", "MSFT"));

        MarketDataRefreshJob.RefreshResult result = job(Duration.ZERO).refreshAll();

        assertEquals(new MarketDataRefreshJob.RefreshResult(2, 0), result);
        verify(marketDataService).fetchHistoricalData("AAPL", WINDOW_START, TODAY, "1day");
        verify(marketDataService).fetchHistoricalData("MSFT", WINDOW_START, TODAY, "1day");
    }

    @Test
    void shouldContinueAfterAssetsFail() {
        when(assetService.findAllProviderSymbols())
                .thenReturn(List.of("AAPL", "MSFT", "TSLA"));
        when(marketDataService.fetchHistoricalData(eq("AAPL"), any(), any(), any()))
                .thenReturn(List.of());
        when(marketDataService.fetchHistoricalData(eq("MSFT"), any(), any(), any()))
                .thenThrow(new MarketDataProviderException("provider down"));
        when(marketDataService.fetchHistoricalData(eq("TSLA"), any(), any(), any()))
                .thenThrow(new IllegalStateException("unexpected"));

        MarketDataRefreshJob.RefreshResult result = job(Duration.ZERO).refreshAll();

        assertEquals(new MarketDataRefreshJob.RefreshResult(1, 2), result);
        verify(marketDataService).fetchHistoricalData("TSLA", WINDOW_START, TODAY, "1day");
    }

    @Test
    void shouldDoNothingWhenThereAreNoAssets() {
        when(assetService.findAllProviderSymbols()).thenReturn(List.of());

        MarketDataRefreshJob.RefreshResult result = job(Duration.ZERO).refreshAll();

        assertEquals(new MarketDataRefreshJob.RefreshResult(0, 0), result);
        verifyNoInteractions(marketDataService);
    }

    @Test
    void shouldStopCleanlyWhenInterrupted() {
        when(assetService.findAllProviderSymbols()).thenReturn(List.of("AAPL", "MSFT"));

        Thread.currentThread().interrupt();
        try {
            MarketDataRefreshJob.RefreshResult result =
                    job(Duration.ofSeconds(1)).refreshAll();

            assertEquals(new MarketDataRefreshJob.RefreshResult(1, 0), result);
            verify(marketDataService).fetchHistoricalData("AAPL", WINDOW_START, TODAY, "1day");
            verify(marketDataService, never())
                    .fetchHistoricalData(eq("MSFT"), any(), any(), any());
        } finally {
            // the job re-sets the flag on purpose; clear it so other tests are unaffected
            Thread.interrupted();
        }
    }
}