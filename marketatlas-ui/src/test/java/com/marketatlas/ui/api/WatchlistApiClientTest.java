package com.marketatlas.ui.api;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withNoContent;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

class WatchlistApiClientTest {

    private static final String AUTH = "Basic YWxpY2U6c2VjcmV0";

    private MockRestServiceServer server;
    private ApiClient client;

    @BeforeEach
    void setUp() {
        RestClient.Builder builder = RestClient.builder().baseUrl("http://api.test");
        server = MockRestServiceServer.bindTo(builder).build();
        client = new ApiClient(builder.build());
    }

    @Test
    void watchlistSendsCredentialsAndParsesEntries() {
        server.expect(requestTo("http://api.test/api/watchlist"))
                .andExpect(method(HttpMethod.GET))
                .andExpect(header(HttpHeaders.AUTHORIZATION, AUTH))
                .andRespond(withSuccess("""
                        [{"assetId":7,"name":"Gold Spot","assetClass":"COMMODITY","currency":"USD",
                          "unit":"troy ounce","latestClose":4033.76,"latestDate":"2026-10-30",
                          "previousClose":4000.00,"change":33.76,"changePercent":0.844,
                          "addedAt":"2026-11-02T08:00:00Z"},
                         {"assetId":9,"name":"No Bars","assetClass":"EQUITY","currency":"USD","unit":null,
                          "latestClose":null,"latestDate":null,"previousClose":null,"change":null,
                          "changePercent":null,"addedAt":"2026-11-02T07:00:00Z"}]
                        """, MediaType.APPLICATION_JSON));

        List<WatchlistEntry> entries = client.watchlist(AUTH);

        assertEquals(2, entries.size());
        WatchlistEntry gold = entries.get(0);
        assertEquals(7L, gold.assetId());
        assertEquals(new BigDecimal("4033.76"), gold.latestClose());
        assertEquals(LocalDate.of(2026, 10, 30), gold.latestDate());
        assertEquals(new BigDecimal("33.76"), gold.change());
        assertNull(entries.get(1).latestClose());
        assertNull(entries.get(1).change());
        server.verify();
    }

    @Test
    void emptyWatchlistIsAnEmptyList() {
        server.expect(requestTo("http://api.test/api/watchlist"))
                .andRespond(withSuccess("[]", MediaType.APPLICATION_JSON));

        assertTrue(client.watchlist(AUTH).isEmpty());
    }

    @Test
    void watchPutsTheAssetWithCredentials() {
        server.expect(requestTo("http://api.test/api/watchlist/7"))
                .andExpect(method(HttpMethod.PUT))
                .andExpect(header(HttpHeaders.AUTHORIZATION, AUTH))
                .andRespond(withNoContent());

        client.watch(AUTH, 7L);

        server.verify();
    }

    @Test
    void fullWatchlistIsReportedAsConflictWithTheApisMessage() {
        server.expect(requestTo("http://api.test/api/watchlist/7"))
                .andRespond(withStatus(HttpStatus.CONFLICT)
                        .contentType(MediaType.APPLICATION_PROBLEM_JSON)
                        .body("{\"title\":\"Watchlist Full\",\"status\":409,"
                                + "\"detail\":\"Watchlist is full (50 assets maximum).\"}"));

        ApiException ex = assertThrows(ApiException.class, () -> client.watch(AUTH, 7L));

        assertEquals(ApiException.Kind.CONFLICT, ex.kind());
        assertEquals("Watchlist is full (50 assets maximum).", ex.getMessage());
    }

    @Test
    void unknownAssetOnWatchIsNotFound() {
        server.expect(requestTo("http://api.test/api/watchlist/999")).andRespond(withStatus(HttpStatus.NOT_FOUND));

        ApiException ex = assertThrows(ApiException.class, () -> client.watch(AUTH, 999L));

        assertEquals(ApiException.Kind.NOT_FOUND, ex.kind());
    }

    @Test
    void unwatchDeletesTheAssetWithCredentials() {
        server.expect(requestTo("http://api.test/api/watchlist/7"))
                .andExpect(method(HttpMethod.DELETE))
                .andExpect(header(HttpHeaders.AUTHORIZATION, AUTH))
                .andRespond(withNoContent());

        client.unwatch(AUTH, 7L);

        server.verify();
    }

    @Test
    void wrongCredentialsOnListAreReported() {
        server.expect(requestTo("http://api.test/api/watchlist")).andRespond(withStatus(HttpStatus.UNAUTHORIZED));

        ApiException ex = assertThrows(ApiException.class, () -> client.watchlist(AUTH));

        assertEquals(ApiException.Kind.INVALID_CREDENTIALS, ex.kind());
    }
}
