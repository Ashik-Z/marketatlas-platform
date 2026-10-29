package com.marketatlas.market.refresh;

import com.marketatlas.common.exception.ProviderRateLimitException;
import com.marketatlas.enums.SeriesFrequency;
import com.marketatlas.market.entity.DataSeries;
import com.marketatlas.market.service.AssetService;
import com.marketatlas.market.service.MarketDataService;
import com.marketatlas.market.service.SeriesService;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class RefreshJobRateLimitTest {

    private final Clock clock = Clock.fixed(Instant.parse("2026-10-29T00:00:00Z"), ZoneOffset.UTC);
    private final RefreshProperties properties =
            new RefreshProperties(true, "0 30 22 * * MON-FRI", 7, Duration.ZERO);

    @Test
    void marketDataRefreshShouldStopCallingTheProviderAfterARateLimit() {
        AssetService assetService = mock(AssetService.class);
        MarketDataService marketDataService = mock(MarketDataService.class);
        when(assetService.findAllProviderSymbols()).thenReturn(List.of("A", "B", "C"));
        doThrow(new ProviderRateLimitException("limit", 60))
                .when(marketDataService).fetchHistoricalData(eq("B"), any(), any(), eq("1day"));

        var result = new MarketDataRefreshJob(assetService, marketDataService, properties, clock).refreshAll();

        assertEquals(1, result.succeeded());
        assertEquals(1, result.failed());
        verify(marketDataService).fetchHistoricalData(eq("A"), any(), any(), eq("1day"));
        verify(marketDataService, never()).fetchHistoricalData(eq("C"), any(), any(), any());
    }

    @Test
    void seriesRefreshShouldStopCallingTheProviderAfterARateLimit() {
        SeriesService seriesService = mock(SeriesService.class);
        DataSeries first = series(1L, "A");
        DataSeries second = series(2L, "B");
        when(seriesService.listSeries()).thenReturn(List.of(first, second));
        doThrow(new ProviderRateLimitException("limit", 60))
                .when(seriesService).refresh(eq(1L), any(), any());

        var result = new SeriesRefreshJob(seriesService, properties, clock).refreshAll();

        assertEquals(0, result.succeeded());
        assertEquals(1, result.failed());
        verify(seriesService, never()).refresh(eq(2L), any(), any());
    }

    private static DataSeries series(Long id, String externalId) {
        DataSeries series = mock(DataSeries.class);
        when(series.getId()).thenReturn(id);
        when(series.getExternalId()).thenReturn(externalId);
        when(series.getFrequency()).thenReturn(SeriesFrequency.DAILY);
        return series;
    }
}