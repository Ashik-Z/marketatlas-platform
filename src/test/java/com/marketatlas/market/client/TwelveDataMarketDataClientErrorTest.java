package com.marketatlas.market.client;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;
import com.marketatlas.common.exception.MarketDataProviderException;
import org.springframework.http.HttpStatus;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.http.HttpMethod.GET;

class TwelveDataMarketDataClientErrorTest {

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
    void shouldRejectProviderErrorResponse() {
        mockServer.expect(requestTo(
                org.hamcrest.Matchers.containsString("/time_series")))
            .andExpect(method(GET))
            .andRespond(withSuccess("""
                {
                    "status": "error",
                    "code": 400,
                    "message": "Invalid symbol"
                }
                """, APPLICATION_JSON));

        MarketDataProviderException exception = assertThrows(
            MarketDataProviderException.class,
            () -> client.getHistoricalData(
                "INVALID",
                LocalDate.of(2026, 10, 1),
                LocalDate.of(2026, 10, 5),
                "1day"
            )
        );

        assertTrue(exception.getMessage().contains("Invalid symbol"));
        mockServer.verify();
    }
    
    @Test
    void shouldRejectEmptyResponse() {
        mockServer.expect(requestTo(
                org.hamcrest.Matchers.containsString("/time_series")))
            .andExpect(method(GET))
            .andRespond(withSuccess("", APPLICATION_JSON));
    
        MarketDataProviderException exception = assertThrows(
            MarketDataProviderException.class,
            () -> client.getHistoricalData(
                "AAPL",
                LocalDate.of(2026, 10, 1),
                LocalDate.of(2026, 10, 5),
                "1day"
            )
        );
    
        assertEquals(
            "Twelve Data returned an empty response.",
            exception.getMessage()
        );
    
        mockServer.verify();
    }

    @Test
    void shouldRejectResponseWithoutMarketData() {
        mockServer.expect(requestTo(
                org.hamcrest.Matchers.containsString("/time_series")))
            .andExpect(method(GET))
            .andRespond(withSuccess("""
                {
                    "symbol": "AAPL",
                    "interval": "1day",
                    "status": "ok"
                }
                """, APPLICATION_JSON));
    
        MarketDataProviderException exception = assertThrows(
            MarketDataProviderException.class,
            () -> client.getHistoricalData(
                "AAPL",
                LocalDate.of(2026, 10, 1),
                LocalDate.of(2026, 10, 5),
                "1day"
            )
        );
    
        assertEquals(
            "Twelve Data response did not contain market data.",
            exception.getMessage()
        );
    
        mockServer.verify();
    }

    @Test
    void shouldWrapHttpErrorResponse() {
        mockServer.expect(requestTo(
                org.hamcrest.Matchers.containsString("/time_series")))
            .andExpect(method(GET))
            .andRespond(withStatus(HttpStatus.TOO_MANY_REQUESTS));
    
        MarketDataProviderException exception = assertThrows(
            MarketDataProviderException.class,
            () -> client.getHistoricalData(
                "AAPL",
                LocalDate.of(2026, 10, 1),
                LocalDate.of(2026, 10, 5),
                "1day"
            )
        );
    
        assertEquals(
            "Twelve Data returned an HTTP error.",
            exception.getMessage()
        );
    
        assertTrue(exception.getCause() instanceof
            org.springframework.web.client.RestClientResponseException);
    
        mockServer.verify();
    }
    
}
