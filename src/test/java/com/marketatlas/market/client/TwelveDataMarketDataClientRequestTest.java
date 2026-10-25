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
}