package com.marketatlas.market.client;

import com.marketatlas.common.exception.MarketDataProviderException;
import com.marketatlas.common.exception.ProviderRateLimitException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.springframework.http.HttpMethod.GET;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

class TwelveDataRateLimitTest {

    private MockRestServiceServer mockServer;
    private TwelveDataMarketDataClient client;

    @BeforeEach
    void setUp() {
        RestClient.Builder builder = RestClient.builder().baseUrl("https://api.twelvedata.com");
        mockServer = MockRestServiceServer.bindTo(builder).build();
        client = new TwelveDataMarketDataClient(builder.build());
        ReflectionTestUtils.setField(client, "apiKey", "test-api-key");
    }

    private void call() {
        client.getHistoricalData("AAPL", LocalDate.of(2026, 10, 1), LocalDate.of(2026, 10, 5), "1day");
    }

    private org.springframework.test.web.client.ResponseActions expectRequest() {
        return mockServer.expect(requestTo(org.hamcrest.Matchers.containsString("/time_series")))
                .andExpect(method(GET));
    }

    @Test
    void shouldReportHttp429WithTheProvidersRetryAfter() {
        expectRequest().andRespond(withStatus(HttpStatus.TOO_MANY_REQUESTS).header("Retry-After", "30"));

        ProviderRateLimitException exception =
                assertThrows(ProviderRateLimitException.class, this::call);

        assertEquals(30, exception.getRetryAfterSeconds());
        mockServer.verify();
    }

    @Test
    void shouldDefaultToOneMinuteWhenThereIsNoRetryAfterHeader() {
        expectRequest().andRespond(withStatus(HttpStatus.TOO_MANY_REQUESTS));

        ProviderRateLimitException exception =
                assertThrows(ProviderRateLimitException.class, this::call);

        assertEquals(60, exception.getRetryAfterSeconds());
    }

    @Test
    void shouldRecognizeARateLimitReportedInsideAnHttp200Body() {
        expectRequest().andRespond(withSuccess("""
                {"status": "error", "code": 429, "message": "You have run out of API credits"}
                """, APPLICATION_JSON));

        assertThrows(ProviderRateLimitException.class, this::call);
    }

    @Test
    void shouldKeepOtherServerErrorsAsPlainProviderErrors() {
        expectRequest().andRespond(withStatus(HttpStatus.INTERNAL_SERVER_ERROR));

        MarketDataProviderException exception =
                assertThrows(MarketDataProviderException.class, this::call);

        assertFalse(exception instanceof ProviderRateLimitException);
    }

    @Test
    void shouldParseRetryAfterDefensively() {
        assertEquals(60, ProviderRateLimitException.parseRetryAfter(null));
        assertEquals(30, ProviderRateLimitException.parseRetryAfter("30"));
        assertEquals(60, ProviderRateLimitException.parseRetryAfter("Wed, 21 Oct 2026 07:28:00 GMT"));
        assertEquals(1, ProviderRateLimitException.parseRetryAfter("0"));
        assertEquals(3600, ProviderRateLimitException.parseRetryAfter("999999"));
    }
}