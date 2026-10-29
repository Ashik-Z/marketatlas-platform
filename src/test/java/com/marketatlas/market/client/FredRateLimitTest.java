package com.marketatlas.market.client;

import com.marketatlas.common.exception.ProviderRateLimitException;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;

class FredRateLimitTest {

    @Test
    void shouldReportAnHttp429AsARateLimitWithRetryAfterAndNeverLeakTheKey() {
        RestClient.Builder builder = RestClient.builder().baseUrl("https://api.stlouisfed.org/fred");
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        FredSeriesClient client = new FredSeriesClient(builder.build(), "secret-test-key");
        server.expect(requestTo(org.hamcrest.Matchers.containsString("/series/observations")))
                .andRespond(withStatus(HttpStatus.TOO_MANY_REQUESTS).header("Retry-After", "45"));

        ProviderRateLimitException exception = assertThrows(ProviderRateLimitException.class,
                () -> client.getObservations("DCOILBRENTEU",
                        LocalDate.of(2026, 10, 1), LocalDate.of(2026, 10, 20)));

        assertEquals(45, exception.getRetryAfterSeconds());
        assertFalse(exception.getMessage().contains("secret-test-key"));
        server.verify();
    }
}