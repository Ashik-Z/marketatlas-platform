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

import java.io.IOException;

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
            .andRespond(withStatus(HttpStatus.BAD_GATEWAY));
    
        MarketDataProviderException exception = assertThrows(
            MarketDataProviderException.class,
            () -> client.getHistoricalData(
                "AAPL",
                LocalDate.of(2026, 10, 1),
                LocalDate.of(2026, 10, 5),
                "1day"
            )
        );

        assertTrue(exception.getMessage()
                .startsWith("Twelve Data returned an HTTP error: 502"));
    
        assertTrue(exception.getCause() instanceof
            org.springframework.web.client.RestClientResponseException);
    
        mockServer.verify();
    }

    @Test
    void shouldWrapConnectionFailure() {
        mockServer.expect(requestTo(
                        org.hamcrest.Matchers.containsString("/time_series")))
                .andExpect(method(GET))
                .andRespond(request -> {
                    throw new IOException("Connection failed");
                });

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
                "Unable to communicate with Twelve Data.",
                exception.getMessage()
        );

        assertTrue(exception.getCause() instanceof
                org.springframework.web.client.ResourceAccessException);

        mockServer.verify();
    }

    @Test
    void shouldRejectResponseWithUnexpectedStatus() {
        mockServer.expect(requestTo(
                        org.hamcrest.Matchers.containsString("/time_series")))
                .andExpect(method(GET))
                .andRespond(withSuccess("""
                {
                    "symbol": "AAPL",
                    "interval": "1day",
                    "status": "pending",
                    "values": []
                }
                """, APPLICATION_JSON));

        MarketDataProviderException exception = assertThrows(
                MarketDataProviderException.class,
                () -> client.getHistoricalData(
                        "AAPL",
                        LocalDate.of(2026, 9, 29),
                        LocalDate.of(2026, 10, 2),
                        "1day"
                )
        );

        assertTrue(exception.getMessage().contains("status"));

        mockServer.verify();
    }

    @Test
    void shouldRejectMarketDataRecordWithMissingFields() {
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
                            "low": 249.0
                        }
                    ]
                }
                """, APPLICATION_JSON));

        MarketDataProviderException exception = assertThrows(
                MarketDataProviderException.class,
                () -> client.getHistoricalData(
                        "AAPL",
                        LocalDate.of(2026, 10, 1),
                        LocalDate.of(2026, 10, 2),
                        "1day"
                )
        );

        assertTrue(exception.getMessage().contains("market data record"));

        mockServer.verify();
    }

    @Test
    void shouldRejectMarketDataRecordWithInconsistentOhlcValues() {
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
                            "high": 248.0,
                            "low": 249.0,
                            "close": 251.0,
                            "volume": 1000
                        }
                    ]
                }
                """, APPLICATION_JSON));

        MarketDataProviderException exception = assertThrows(
                MarketDataProviderException.class,
                () -> client.getHistoricalData(
                        "AAPL",
                        LocalDate.of(2026, 10, 1),
                        LocalDate.of(2026, 10, 2),
                        "1day"
                )
        );

        assertTrue(exception.getMessage().contains("invalid market data record"));

        mockServer.verify();
    }

    @Test
    void shouldTreatHttp400NoDataResponseAsEmptyResult() {
        mockServer.expect(requestTo(
                        org.hamcrest.Matchers.containsString("/time_series")))
                .andExpect(method(GET))
                .andRespond(withStatus(HttpStatus.BAD_REQUEST)
                        .contentType(APPLICATION_JSON)
                        .body("""
                {
                    "code": 400,
                    "message": "No data is available on the specified dates. Try setting different start/end dates.",
                    "status": "error",
                    "meta": {"symbol": "AAPL", "interval": "1day", "exchange": ""}
                }
                """));

        assertTrue(client.getHistoricalData(
                "AAPL",
                LocalDate.of(2026, 10, 17),
                LocalDate.of(2026, 10, 18),
                "1day"
        ).isEmpty());

        mockServer.verify();
    }

    @Test
    void shouldTreatNoDataErrorBodyOnHttp200AsEmptyResult() {
        mockServer.expect(requestTo(
                        org.hamcrest.Matchers.containsString("/time_series")))
                .andExpect(method(GET))
                .andRespond(withSuccess("""
                {
                    "status": "error",
                    "code": 400,
                    "message": "No data is available on the specified dates."
                }
                """, APPLICATION_JSON));

        assertTrue(client.getHistoricalData(
                "AAPL",
                LocalDate.of(2026, 10, 17),
                LocalDate.of(2026, 10, 18),
                "1day"
        ).isEmpty());

        mockServer.verify();
    }

    @Test
    void shouldStillRejectOtherHttp400Errors() {
        mockServer.expect(requestTo(
                        org.hamcrest.Matchers.containsString("/time_series")))
                .andExpect(method(GET))
                .andRespond(withStatus(HttpStatus.BAD_REQUEST)
                        .contentType(APPLICATION_JSON)
                        .body("""
                {"code": 400, "message": "symbol is missing or invalid", "status": "error"}
                """));

        MarketDataProviderException exception = assertThrows(
                MarketDataProviderException.class,
                () -> client.getHistoricalData(
                        "BAD",
                        LocalDate.of(2026, 10, 1),
                        LocalDate.of(2026, 10, 2),
                        "1day"
                )
        );

        assertTrue(exception.getMessage().contains("400"));
        assertTrue(exception.getMessage().contains("symbol is missing or invalid"));

        mockServer.verify();
    }

    @Test
    void shouldTruncateLongProviderErrorBodies() {
        mockServer.expect(requestTo(
                        org.hamcrest.Matchers.containsString("/time_series")))
                .andExpect(method(GET))
                .andRespond(withStatus(HttpStatus.INTERNAL_SERVER_ERROR)
                        .contentType(APPLICATION_JSON)
                        .body("x".repeat(5000)));

        MarketDataProviderException exception = assertThrows(
                MarketDataProviderException.class,
                () -> client.getHistoricalData(
                        "AAPL",
                        LocalDate.of(2026, 10, 1),
                        LocalDate.of(2026, 10, 2),
                        "1day"
                )
        );

        assertTrue(exception.getMessage().length() < 400);

        mockServer.verify();
    }
}
