package com.marketatlas.market.refresh;

import com.marketatlas.market.service.AssetService;
import com.marketatlas.market.service.MarketDataService;
import org.junit.jupiter.api.Test;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

import java.time.Clock;
import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

class RefreshConfigurationTest {

    @Configuration
    @EnableConfigurationProperties(RefreshProperties.class)
    @Import(MarketDataRefreshJob.class)
    static class TestConfig {

        @Bean
        AssetService assetService() {
            return mock(AssetService.class);
        }

        @Bean
        MarketDataService marketDataService() {
            return mock(MarketDataService.class);
        }

        @Bean
        Clock clock() {
            return Clock.systemUTC();
        }
    }

    private final ApplicationContextRunner runner =
            new ApplicationContextRunner().withUserConfiguration(TestConfig.class);

    @Test
    void shouldBeOffByDefaultWithSafeDefaults() {
        runner.run(context -> {
            RefreshProperties properties = context.getBean(RefreshProperties.class);

            assertThat(properties.enabled()).isFalse();
            assertThat(properties.cron()).isEqualTo("0 30 22 * * MON-FRI");
            assertThat(properties.lookbackDays()).isEqualTo(7);
            assertThat(properties.delayBetweenAssets()).isEqualTo(Duration.ofSeconds(8));
            assertThat(context).doesNotHaveBean(MarketDataRefreshJob.class);
        });
    }

    @Test
    void shouldCreateTheJobWhenEnabledAndBindOverrides() {
        runner.withPropertyValues(
                        "marketatlas.refresh.enabled=true",
                        "marketatlas.refresh.lookback-days=3",
                        "marketatlas.refresh.delay-between-assets=2s")
                .run(context -> {
                    RefreshProperties properties = context.getBean(RefreshProperties.class);

                    assertThat(context).hasSingleBean(MarketDataRefreshJob.class);
                    assertThat(properties.lookbackDays()).isEqualTo(3);
                    assertThat(properties.delayBetweenAssets()).isEqualTo(Duration.ofSeconds(2));
                });
    }

    @Test
    void shouldNotCreateTheJobWhenExplicitlyDisabled() {
        runner.withPropertyValues("marketatlas.refresh.enabled=false")
                .run(context -> assertThat(context).doesNotHaveBean(MarketDataRefreshJob.class));
    }

    @Test
    void shouldRejectLookbackOutsideAllowedRange() {
        runner.withPropertyValues("marketatlas.refresh.lookback-days=0")
                .run(context -> assertThat(context).hasFailed());

        runner.withPropertyValues("marketatlas.refresh.lookback-days=31")
                .run(context -> assertThat(context).hasFailed());
    }

    @Test
    void shouldRejectNegativeDelay() {
        runner.withPropertyValues("marketatlas.refresh.delay-between-assets=-1s")
                .run(context -> assertThat(context).hasFailed());
    }
}