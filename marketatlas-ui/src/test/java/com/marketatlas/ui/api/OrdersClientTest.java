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
import java.time.Instant;
import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.jsonPath;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

class OrdersClientTest {

    private static final String AUTH = "Basic dGVzdDp0ZXN0LXBhc3M=";
    private static final String TRADE = "{\"id\":11,\"accountId\":3,\"assetId\":7,\"side\":\"BUY\",\"quantity\":0.050000,"
            + "\"price\":2200.000000,\"commission\":1.000000,\"priceDate\":\"2026-10-30\",\"executedAt\":\"2026-10-31T08:00:00.123456Z\"}";

    private MockRestServiceServer server;
    private ApiClient client;

    @BeforeEach
    void setUp() {
        RestClient.Builder builder = RestClient.builder().baseUrl("http://api.test");
        server = MockRestServiceServer.bindTo(builder).build();
        client = new ApiClient(builder.build());
    }

    @Test
    void placeOrderSendsKeyAuthAndBodyAndParsesTheTrade() {
        server.expect(requestTo("http://api.test/api/accounts/3/orders"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(header(HttpHeaders.AUTHORIZATION, AUTH))
                .andExpect(header("Idempotency-Key", "key-1"))
                .andExpect(jsonPath("$.assetId").value(7))
                .andExpect(jsonPath("$.side").value("BUY"))
                .andExpect(jsonPath("$.quantity").value(0.05))
                .andRespond(withStatus(HttpStatus.CREATED).contentType(MediaType.APPLICATION_JSON).body(TRADE));

        TradeRecord t = client.placeOrder(AUTH, 3L, "key-1", 7L, "BUY", new BigDecimal("0.05"));

        assertEquals(11L, t.id());
        assertEquals("BUY", t.side());
        assertEquals(LocalDate.of(2026, 10, 30), t.priceDate());
        assertEquals(Instant.parse("2026-10-31T08:00:00.123456Z"), t.executedAt());
        server.verify();
    }

    @Test
    void rejectedOrderShowsTheRealReason() {
        server.expect(requestTo("http://api.test/api/accounts/3/orders"))
                .andRespond(withStatus(HttpStatus.CONFLICT).contentType(MediaType.APPLICATION_PROBLEM_JSON)
                        .body("{\"status\":409,\"detail\":\"Insufficient cash for this order\"}"));

        ApiException ex = assertThrows(ApiException.class,
                () -> client.placeOrder(AUTH, 3L, "key-2", 7L, "BUY", BigDecimal.TEN));

        assertEquals(ApiException.Kind.CONFLICT, ex.kind());
        assertEquals("Insufficient cash for this order", ex.getMessage());
    }

    @Test
    void unknownAssetIsNotFound() {
        server.expect(requestTo("http://api.test/api/accounts/3/orders"))
                .andRespond(withStatus(HttpStatus.NOT_FOUND).contentType(MediaType.APPLICATION_PROBLEM_JSON)
                        .body("{\"status\":404,\"detail\":\"Asset not found: 99\"}"));

        ApiException ex = assertThrows(ApiException.class,
                () -> client.placeOrder(AUTH, 3L, "key-3", 99L, "BUY", BigDecimal.ONE));

        assertEquals(ApiException.Kind.NOT_FOUND, ex.kind());
        assertEquals("Asset not found: 99", ex.getMessage());
    }

    @Test
    void orderRateLimitKeepsRetryAfter() {
        server.expect(requestTo("http://api.test/api/accounts/3/orders"))
                .andRespond(withStatus(HttpStatus.TOO_MANY_REQUESTS).header(HttpHeaders.RETRY_AFTER, "12"));

        ApiException ex = assertThrows(ApiException.class,
                () -> client.placeOrder(AUTH, 3L, "key-4", 7L, "SELL", BigDecimal.ONE));

        assertEquals(ApiException.Kind.RATE_LIMITED, ex.kind());
        assertEquals(12L, ex.retryAfterSeconds());
    }

    @Test
    void tradesSendsPagingAndParsesThePage() {
        String body = "{\"content\":[" + TRADE + "],\"page\":0,\"size\":50,\"totalElements\":1,\"totalPages\":1}";
        server.expect(requestTo("http://api.test/api/accounts/3/trades?page=0&size=50"))
                .andExpect(header(HttpHeaders.AUTHORIZATION, AUTH))
                .andRespond(withSuccess(body, MediaType.APPLICATION_JSON));

        TradePage page = client.trades(AUTH, 3L, 0, 50);

        assertEquals(1, page.content().size());
        assertEquals(1L, page.totalElements());
        assertEquals(new BigDecimal("2200.000000"), page.content().get(0).price());
        server.verify();
    }
}