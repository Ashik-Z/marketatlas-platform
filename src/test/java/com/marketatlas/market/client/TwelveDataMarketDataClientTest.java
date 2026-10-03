package com.marketatlas.market.client;

import static org.junit.jupiter.api.Assertions.*;

import java.time.LocalDate;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import com.marketatlas.market.dto.MarketDataDTO;

@SpringBootTest
class TwelveDataMarketDataClientTest {

    @Autowired
    private MarketDataClient marketDataClient;

    @Test
    void shouldFetchHistoricalMarketData() {

        List<MarketDataDTO> marketData = marketDataClient.getHistoricalData(
            "AAPL",
            LocalDate.of(2026, 9, 29),
            LocalDate.of(2026, 10, 2),
            "1day"
        );

        assertNotNull(marketData);
        assertFalse(marketData.isEmpty());

        MarketDataDTO firstRecord = marketData.get(0);

        assertNotNull(firstRecord.getOpen());
        assertNotNull(firstRecord.getHigh());
        assertNotNull(firstRecord.getLow());
        assertNotNull(firstRecord.getClose());
        assertNotNull(firstRecord.getTimestamp());
    }
}