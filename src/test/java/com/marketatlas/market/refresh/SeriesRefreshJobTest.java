package com.marketatlas.market.refresh;

import com.marketatlas.common.exception.MarketDataProviderException;
import com.marketatlas.enums.CopyrightClass;
import com.marketatlas.enums.SeriesFrequency;
import com.marketatlas.enums.SeriesProvider;
import com.marketatlas.market.entity.DataSeries;
import com.marketatlas.market.service.SeriesService;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class SeriesRefreshJobTest {

    private static final LocalDate TODAY = LocalDate.of(2026, 10, 26);
    private static final Clock CLOCK =
            Clock.fixed(Instant.parse("2026-10-26T12:00:00Z"), ZoneOffset.UTC);

    private final SeriesService seriesService = mock(SeriesService.class);
    private final RefreshProperties properties =
            new RefreshProperties(true, "0 30 22 * * MON-FRI", 7, Duration.ZERO);
    private final SeriesRefreshJob job = new SeriesRefreshJob(seriesService, properties, CLOCK);

    private static DataSeries series(long id, String externalId, SeriesFrequency frequency) {
        DataSeries series = new DataSeries(SeriesProvider.FRED, externalId, "Name", frequency,
                "units", "source", CopyrightClass.PUBLIC_DOMAIN_CITATION_REQUESTED, "attribution");
        ReflectionTestUtils.setField(series, "id", id);
        return series;
    }

    @Test
    void shouldUseLongerLookbackForMonthlySeries() {
        when(seriesService.listSeries()).thenReturn(List.of(
                series(1L, "CPIAUCSL", SeriesFrequency.MONTHLY),
                series(2L, "DHHNGSP", SeriesFrequency.DAILY)));

        MarketDataRefreshJob.RefreshResult result = job.refreshAll();

        assertEquals(new MarketDataRefreshJob.RefreshResult(2, 0), result);
        verify(seriesService).refresh(1L, TODAY.minusDays(SeriesRefreshJob.MONTHLY_LOOKBACK_DAYS), TODAY);
        verify(seriesService).refresh(2L, TODAY.minusDays(7), TODAY);
    }

    @Test
    void shouldContinueAfterASeriesFails() {
        when(seriesService.listSeries()).thenReturn(List.of(
                series(1L, "DCOILBRENTEU", SeriesFrequency.DAILY),
                series(2L, "DHHNGSP", SeriesFrequency.DAILY)));
        when(seriesService.refresh(1L, TODAY.minusDays(7), TODAY))
                .thenThrow(new MarketDataProviderException("FRED rate limit exceeded (HTTP 429)."));

        MarketDataRefreshJob.RefreshResult result = job.refreshAll();

        assertEquals(new MarketDataRefreshJob.RefreshResult(1, 1), result);
        verify(seriesService).refresh(2L, TODAY.minusDays(7), TODAY);
    }

    @Test
    void shouldDoNothingWhenThereAreNoSeries() {
        when(seriesService.listSeries()).thenReturn(List.of());

        MarketDataRefreshJob.RefreshResult result = job.refreshAll();

        assertEquals(new MarketDataRefreshJob.RefreshResult(0, 0), result);
        verify(seriesService, never()).refresh(any(), any(), any());
    }
}