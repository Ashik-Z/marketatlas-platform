package com.marketatlas.market.client;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.springframework.http.HttpMethod.GET;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import com.marketatlas.market.dto.MarketDataDTO;
import java.util.List;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.queryParam;

class TwelveDataMarketDataClientRequestTest {

    private MockRestServiceServer mockServer;
    private TwelveDataMarketDataClient client;

    @BeforeEach
    void setUp() {
        RestClient.Builder builder = RestClient.builder()
                .baseUrl("https://api.twelvedata.com");
        mockServer = MockRestServiceServer.bindTo(builder).build();
        client = new TwelveDataMarketDataClient(builder.build());
        ReflectionTestUtils.setField(client, "apiKey", "test-api-key");
    }

    @Test
    void shouldSendApiKeyInHeaderAndNeverInUrl() {
        mockServer.expect(method(GET))
                .andExpect(header("Authorization", "apikey test-api-key"))
                .andExpect(request -> assertFalse(
                        request.getURI().toString().contains("test-api-key")))
                .andExpect(request -> assertFalse(
                        request.getURI().toString().contains("apikey")))
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
                                    "close": 251.0,
                                    "volume": 1000
                                }
                            ]
                        }
                        """, APPLICATION_JSON));

        client.getHistoricalData("AAPL",
                LocalDate.of(2026, 10, 1), LocalDate.of(2026, 10, 2), "1day");

        mockServer.verify();
    }

    @Test
    void shouldAskProviderForOneDayPastEndBecauseEndDateIsExclusive() {
        mockServer.expect(method(GET))
                .andExpect(queryParam("start_date", "2024-02-05"))
                .andExpect(queryParam("end_date", "2024-02-10"))
                .andRespond(withSuccess("""
                        {"symbol": "AAPL", "interval": "1day", "status": "ok", "values": []}
                        """, APPLICATION_JSON));

        client.getHistoricalData("AAPL",
                LocalDate.of(2024, 2, 5), LocalDate.of(2024, 2, 9), "1day");

        mockServer.verify();
    }

    @Test
    void shouldDropBarsOutsideTheRequestedRange() {
        mockServer.expect(method(GET))
                .andRespond(withSuccess("""
                        {
                            "symbol": "AAPL",
                            "interval": "1day",
                            "status": "ok",
                            "values": [
                                {"datetime": "2026-10-03", "open": 12.0, "high": 13.0, "low": 11.0, "close": 12.5, "volume": 10},
                                {"datetime": "2026-10-02", "open": 11.0, "high": 12.0, "low": 10.0, "close": 11.5, "volume": 10},
                                {"datetime": "2026-10-01", "open": 10.0, "high": 11.0, "low": 9.0, "close": 10.5, "volume": 10},
                                {"datetime": "2026-09-30", "open": 9.0, "high": 10.0, "low": 8.0, "close": 9.5, "volume": 10}
                            ]
                        }
                        """, APPLICATION_JSON));

        List<MarketDataDTO> bars = client.getHistoricalData("AAPL",
                LocalDate.of(2026, 10, 1), LocalDate.of(2026, 10, 2), "1day");

        assertEquals(List.of(LocalDate.of(2026, 10, 2), LocalDate.of(2026, 10, 1)),
                bars.stream().map(MarketDataDTO::getTimestamp).toList());
    }

    @Test
    void shouldParseCommodityBarWithoutVolume() {
        mockServer.expect(method(GET))
                .andRespond(withSuccess("""
                        {
                            "meta": {"symbol": "XAU/USD", "interval": "1day", "type": "Precious Metal"},
                            "values": [
                                {"datetime": "2026-10-25", "open": "3993.61501", "high": "3993.92923", "low": "3993.51289", "close": "3993.80662"}
                            ],
                            "status": "ok"
                        }
                        """, APPLICATION_JSON));

        List<MarketDataDTO> bars = client.getHistoricalData("XAU/USD",
                LocalDate.of(2026, 10, 25), LocalDate.of(2026, 10, 25), "1day");

        assertEquals(1, bars.size());
        assertEquals(3993.80662, bars.get(0).getClose());
        assertEquals(null, bars.get(0).getVolume());
    }
}