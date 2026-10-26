package com.marketatlas.market.client;

import com.marketatlas.common.exception.MarketDataProviderException;
import com.marketatlas.market.dto.SeriesObservationDTO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import java.io.IOException;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.http.HttpMethod.GET;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.queryParam;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

class FredSeriesClientTest {

    private static final String KEY = "test-fred-key";
    private static final LocalDate START = LocalDate.of(2026, 7, 1);
    private static final LocalDate END = LocalDate.of(2026, 9, 1);

    private MockRestServiceServer mockServer;
    private FredSeriesClient client;

    @BeforeEach
    void setUp() {
        RestClient.Builder builder = RestClient.builder().baseUrl("https://api.stlouisfed.org/fred");
        mockServer = MockRestServiceServer.bindTo(builder).build();
        client = new FredSeriesClient(builder.build(), KEY);
    }

    private static String observations(String... items) {
        return "{\"realtime_start\":\"2026-10-26\",\"observations\":[" + String.join(",", items) + "]}";
    }

    private static String obs(String date, String value) {
        return "{\"realtime_start\":\"2026-10-26\",\"realtime_end\":\"2026-10-26\",\"date\":\""
                + date + "\",\"value\":\"" + value + "\"}";
    }

    @Test
    void shouldParseValuesAndTreatDotAsMissing() {
        mockServer.expect(method(GET)).andRespond(withSuccess(observations(
                        obs("2026-07-01", "334.131"), obs("2026-08-01", "."), obs("2026-09-01", "335.5")),
                APPLICATION_JSON));

        List<SeriesObservationDTO> result = client.getObservations("CPIAUCSL", START, END);

        assertEquals(3, result.size());
        assertEquals(0, new BigDecimal("334.131").compareTo(result.get(0).value()));
        assertNull(result.get(1).value());
        assertEquals(LocalDate.of(2026, 8, 1), result.get(1).date());
        assertEquals(0, new BigDecimal("335.5").compareTo(result.get(2).value()));
    }

    @Test
    void shouldSendSeriesRangeAndKeyAsQueryParameters() {
        mockServer.expect(method(GET))
                .andExpect(queryParam("series_id", "CPIAUCSL"))
                .andExpect(queryParam("observation_start", "2026-07-01"))
                .andExpect(queryParam("observation_end", "2026-09-01"))
                .andExpect(queryParam("file_type", "json"))
                .andExpect(queryParam("api_key", KEY))
                .andRespond(withSuccess(observations(), APPLICATION_JSON));

        assertTrue(client.getObservations("CPIAUCSL", START, END).isEmpty());
        mockServer.verify();
    }

    @Test
    void shouldDropObservationsOutsideRequestedRange() {
        mockServer.expect(method(GET)).andRespond(withSuccess(observations(
                        obs("2026-09-02", "9.0"), obs("2026-09-01", "8.0"),
                        obs("2026-07-01", "7.0"), obs("2026-06-30", "6.0")),
                APPLICATION_JSON));

        List<SeriesObservationDTO> result = client.getObservations("DHHNGSP", START, END);

        assertEquals(List.of(LocalDate.of(2026, 7, 1), LocalDate.of(2026, 9, 1)),
                result.stream().map(SeriesObservationDTO::date).toList());
    }

    @Test
    void shouldRejectMalformedValue() {
        mockServer.expect(method(GET)).andRespond(
                withSuccess(observations(obs("2026-07-01", "abc")), APPLICATION_JSON));

        assertThrows(MarketDataProviderException.class,
                () -> client.getObservations("CPIAUCSL", START, END));
    }

    @Test
    void shouldReportProviderErrorPayload() {
        mockServer.expect(method(GET)).andRespond(withStatus(HttpStatus.BAD_REQUEST)
                .contentType(APPLICATION_JSON)
                .body("{\"error_code\":400,\"error_message\":\"Bad Request. The series does not exist.\"}"));

        MarketDataProviderException exception = assertThrows(MarketDataProviderException.class,
                () -> client.getObservations("NOPE", START, END));

        assertTrue(exception.getMessage().contains("400"));
        assertTrue(exception.getMessage().contains("does not exist"));
    }

    @Test
    void shouldReportRateLimit() {
        mockServer.expect(method(GET)).andRespond(withStatus(HttpStatus.TOO_MANY_REQUESTS));

        MarketDataProviderException exception = assertThrows(MarketDataProviderException.class,
                () -> client.getObservations("CPIAUCSL", START, END));

        assertTrue(exception.getMessage().contains("429"));
    }

    @Test
    void shouldNeverLeakApiKeyOnConnectionFailure() {
        mockServer.expect(method(GET)).andRespond(request -> {
            throw new IOException("Connection failed");
        });

        MarketDataProviderException exception = assertThrows(MarketDataProviderException.class,
                () -> client.getObservations("CPIAUCSL", START, END));

        assertEquals("Unable to communicate with FRED.", exception.getMessage());
        assertFalse(exception.getMessage().contains(KEY));
        assertNull(exception.getCause());
    }

    @Test
    void shouldFailClearlyWhenApiKeyIsNotConfigured() {
        FredSeriesClient unconfigured = new FredSeriesClient(RestClient.builder().build(), "  ");

        MarketDataProviderException exception = assertThrows(MarketDataProviderException.class,
                () -> unconfigured.getObservations("CPIAUCSL", START, END));

        assertEquals("FRED API key is not configured.", exception.getMessage());
    }
}