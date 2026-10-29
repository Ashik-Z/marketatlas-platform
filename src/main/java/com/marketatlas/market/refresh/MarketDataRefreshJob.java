package com.marketatlas.market.refresh;

import com.marketatlas.market.service.AssetService;
import com.marketatlas.market.service.MarketDataService;
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
public class MarketDataRefreshJob {

    private static final Logger log = LoggerFactory.getLogger(MarketDataRefreshJob.class);

    public record RefreshResult(int succeeded, int failed) {
    }

    private final AssetService assetService;
    private final MarketDataService marketDataService;
    private final RefreshProperties properties;
    private final Clock clock;

    public MarketDataRefreshJob(AssetService assetService,
                                MarketDataService marketDataService,
                                RefreshProperties properties,
                                Clock clock) {
        this.assetService = assetService;
        this.marketDataService = marketDataService;
        this.properties = properties;
        this.clock = clock;
    }

    // fixedDelay-style safety: Spring never starts this method again on the
    // same scheduler thread while it is still running.
    @Scheduled(cron = "${marketatlas.refresh.cron:0 30 22 * * MON-FRI}", zone = "UTC")
    public void scheduledRun() {
        refreshAll();
    }

    public RefreshResult refreshAll() {
        List<String> symbols = assetService.findAllProviderSymbols();
        LocalDate today = LocalDate.now(clock);
        LocalDate from = today.minusDays(properties.lookbackDays());

        log.info("Refresh started: {} assets, {} to {}", symbols.size(), from, today);

        int succeeded = 0;
        int failed = 0;

        for (int i = 0; i < symbols.size(); i++) {
            if (i > 0 && !pause()) {
                log.warn("Refresh interrupted after {} of {} assets", i, symbols.size());
                break;
            }

            String symbol = symbols.get(i);
            try {
                marketDataService.fetchHistoricalData(symbol, from, today, "1day");
                succeeded++;
            } catch (ProviderRateLimitException exception) {
                failed++;
                log.warn("Refresh stopped early at {}: rate limited, {} assets not attempted. {}",
                        symbol, symbols.size() - i - 1, exception.getMessage());
                break;
            } catch (RuntimeException exception) {
                failed++;
                log.warn("Refresh failed for {}: {}", symbol, exception.getMessage());
            }
        }

        log.info("Refresh finished: {} succeeded, {} failed", succeeded, failed);
        return new RefreshResult(succeeded, failed);
    }

    /** Waits between assets to respect the provider's rate limit. */
    private boolean pause() {
        long millis = properties.delayBetweenAssets().toMillis();
        if (millis <= 0) {
            return true;
        }
        try {
            Thread.sleep(millis);
            return true;
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            return false;
        }
    }
}