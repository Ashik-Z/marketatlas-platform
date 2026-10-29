package com.marketatlas.market.refresh;

import com.marketatlas.enums.SeriesFrequency;
import com.marketatlas.market.entity.DataSeries;
import com.marketatlas.market.service.SeriesService;
import com.marketatlas.common.exception.ProviderRateLimitException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.LocalDate;
import java.util.List;

@Component
@ConditionalOnProperty(name = "marketatlas.refresh.enabled", havingValue = "true")
public class SeriesRefreshJob {

    private static final Logger log = LoggerFactory.getLogger(SeriesRefreshJob.class);

    /** Monthly series are published with a lag and revised, so look further back for them. */
    static final int MONTHLY_LOOKBACK_DAYS = 120;

    private final SeriesService seriesService;
    private final RefreshProperties properties;
    private final Clock clock;

    public SeriesRefreshJob(SeriesService seriesService, RefreshProperties properties, Clock clock) {
        this.seriesService = seriesService;
        this.properties = properties;
        this.clock = clock;
    }

    @Scheduled(cron = "${marketatlas.refresh.cron:0 30 22 * * MON-FRI}", zone = "UTC")
    public void scheduledRun() {
        refreshAll();
    }

    public MarketDataRefreshJob.RefreshResult refreshAll() {
        List<DataSeries> allSeries = seriesService.listSeries();
        LocalDate today = LocalDate.now(clock);

        log.info("Series refresh started: {} series, up to {}", allSeries.size(), today);

        int succeeded = 0;
        int failed = 0;

        for (DataSeries series : allSeries) {
            int lookback = series.getFrequency() == SeriesFrequency.MONTHLY
                    ? MONTHLY_LOOKBACK_DAYS
                    : properties.lookbackDays();
            try {
                seriesService.refresh(series.getId(), today.minusDays(lookback), today);
                succeeded++;
            } catch (ProviderRateLimitException exception) {
                failed++;
                log.warn("Series refresh stopped early at {}: rate limited. {}",
                        series.getExternalId(), exception.getMessage());
                break;
            } catch (RuntimeException exception) {
                failed++;
                log.warn("Series refresh failed for {}: {}", series.getExternalId(), exception.getMessage());
            }
        }

        log.info("Series refresh finished: {} succeeded, {} failed", succeeded, failed);
        return new MarketDataRefreshJob.RefreshResult(succeeded, failed);
    }
}