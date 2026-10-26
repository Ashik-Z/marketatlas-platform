package com.marketatlas.market.refresh;

import com.marketatlas.market.TestPostgresConfiguration;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Import;

import static org.junit.jupiter.api.Assertions.assertNotNull;

@SpringBootTest(properties = "marketatlas.refresh.enabled=true")
@Import(TestPostgresConfiguration.class)
class SeriesRefreshJobWiringTest {

    @Autowired
    private ApplicationContext context;

    @Test
    void shouldCreateBothRefreshJobsWhenEnabled() {
        assertNotNull(context.getBean(SeriesRefreshJob.class));
        assertNotNull(context.getBean(MarketDataRefreshJob.class));
    }
}