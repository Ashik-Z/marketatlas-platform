package com.marketatlas.market.client;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;
import static org.springframework.http.HttpMethod.GET;

import java.time.LocalDate;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import com.marketatlas.market.dto.MarketDataDTO;

class TwelveDataMarketDataClientTest {

    private MockRestServiceServer mockServer;
    private TwelveDataMarketDataClient marketDataClient;

    @BeforeEach
    void setUp() {
        RestClient.Builder builder = RestClient.builder()
                .baseUrl("https://api.twelvedata.com");

        mockServer = MockRestServiceServer.bindTo(builder).build();

        marketDataClient = new TwelveDataMarketDataClient(builder.build());
        ReflectionTestUtils.setField(
                marketDataClient,
                "apiKey",
                "test-api-key"
        );
    }

    @Test
    void shouldFetchHistoricalMarketData() {
        mockServer.expect(requestTo(
                        org.hamcrest.Matchers.containsString("/time_series")))
                .andExpect(method(GET))
                .andRespond(withSuccess("""
                {
                    "symbol": "AAPL",
                    "interval": "1day",
                    "status": "ok",
                    "values": [
                        {
                            "datetime": "2026-10-01",
                            "open": 250.0,
                            "high": 255.0,
                            "low": 249.0,
                            "close": 253.0,
                            "volume": 100000
                        }
                    ]
                }
                """, APPLICATION_JSON));

        List<MarketDataDTO> marketData =
                marketDataClient.getHistoricalData(
                        "AAPL",
                        LocalDate.of(2026, 9, 29),
                        LocalDate.of(2026, 10, 2),
                        "1day"
                );

        assertNotNull(marketData);
        assertFalse(marketData.isEmpty());
        assertEquals(1, marketData.size());

        MarketDataDTO firstRecord = marketData.get(0);

        assertNotNull(firstRecord.getOpen());
        assertNotNull(firstRecord.getHigh());
        assertNotNull(firstRecord.getLow());
        assertNotNull(firstRecord.getClose());
        assertNotNull(firstRecord.getTimestamp());

        assertEquals(250.0, firstRecord.getOpen());
        assertEquals(255.0, firstRecord.getHigh());
        assertEquals(249.0, firstRecord.getLow());
        assertEquals(253.0, firstRecord.getClose());

        mockServer.verify();
    }
}