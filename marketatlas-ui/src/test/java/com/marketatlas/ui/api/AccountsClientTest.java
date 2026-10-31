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
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.jsonPath;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

class AccountsClientTest {

    private static final String AUTH = "Basic dGVzdDp0ZXN0LXBhc3M=";
    private static final String ACCOUNT = "{\"id\":3,\"name\":\"Main\",\"currency\":\"USD\","
            + "\"startingBalance\":1000.000000,\"cashBalance\":900.000000,\"createdAt\":\"2026-10-31T08:00:00Z\"}";

    private MockRestServiceServer server;
    private ApiClient client;

    @BeforeEach
    void setUp() {
        RestClient.Builder builder = RestClient.builder().baseUrl("http://api.test");
        server = MockRestServiceServer.bindTo(builder).build();
        client = new ApiClient(builder.build());
    }

    @Test
    void listAccountsSendsTheAuthHeaderAndParses() {
        server.expect(requestTo("http://api.test/api/accounts"))
                .andExpect(method(HttpMethod.GET))
                .andExpect(header(HttpHeaders.AUTHORIZATION, AUTH))
                .andRespond(withSuccess("[" + ACCOUNT + "]", MediaType.APPLICATION_JSON));

        List<AccountSummary> accounts = client.listAccounts(AUTH);

        assertEquals(1, accounts.size());
        assertEquals("Main", accounts.get(0).name());
        assertEquals(new BigDecimal("900.000000"), accounts.get(0).cashBalance());
        server.verify();
    }

    @Test
    void createAccountPostsTheFieldsAndReturnsTheAccount() {
        server.expect(requestTo("http://api.test/api/accounts"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(header(HttpHeaders.AUTHORIZATION, AUTH))
                .andExpect(jsonPath("$.name").value("Main"))
                .andExpect(jsonPath("$.currency").value("USD"))
                .andExpect(jsonPath("$.startingBalance").value(1000))
                .andRespond(withStatus(HttpStatus.CREATED).contentType(MediaType.APPLICATION_JSON).body(ACCOUNT));

        AccountSummary created = client.createAccount(AUTH, "Main", "USD", new BigDecimal("1000"));

        assertEquals(3L, created.id());
        server.verify();
    }

    @Test
    void createAccountValidationErrorCarriesTheDetail() {
        server.expect(requestTo("http://api.test/api/accounts"))
                .andRespond(withStatus(HttpStatus.BAD_REQUEST).contentType(MediaType.APPLICATION_PROBLEM_JSON)
                        .body("{\"status\":400,\"detail\":\"currency: must be a 3-letter uppercase ISO 4217 code\"}"));

        ApiException ex = assertThrows(ApiException.class,
                () -> client.createAccount(AUTH, "Main", "usd", BigDecimal.TEN));

        assertEquals(ApiException.Kind.INVALID_INPUT, ex.kind());
        assertTrue(ex.getMessage().contains("3-letter"));
    }

    @Test
    void portfolioParsesPositionsIncludingAnUnpricedOne() {
        String body = "{\"accountId\":3,\"currency\":\"USD\",\"startingBalance\":1000,\"cashBalance\":900,"
                + "\"positionsValue\":110,\"totalValue\":1010,\"profitLoss\":10,\"realizedPnl\":2,\"unrealizedPnl\":8,"
                + "\"valuationComplete\":false,\"positions\":["
                + "{\"assetId\":7,\"assetName\":\"Gold Spot\",\"quantity\":0.05,\"averageCost\":2000,\"costBasis\":100,"
                + "\"latestPrice\":2200,\"priceDate\":\"2026-10-30\",\"marketValue\":110,\"unrealizedPnl\":10,\"stale\":false},"
                + "{\"assetId\":1,\"assetName\":\"Apple Inc.\",\"quantity\":2,\"averageCost\":200,\"costBasis\":400,"
                + "\"latestPrice\":null,\"priceDate\":null,\"marketValue\":null,\"unrealizedPnl\":null,\"stale\":true}]}";
        server.expect(requestTo("http://api.test/api/accounts/3/portfolio"))
                .andExpect(header(HttpHeaders.AUTHORIZATION, AUTH))
                .andRespond(withSuccess(body, MediaType.APPLICATION_JSON));

        Portfolio p = client.portfolio(AUTH, 3L);

        assertEquals(new BigDecimal("1010"), p.totalValue());
        assertEquals(false, p.valuationComplete());
        assertEquals(2, p.positions().size());
        assertEquals(new BigDecimal("0.05"), p.positions().get(0).quantity());
        assertNull(p.positions().get(1).latestPrice());
        assertEquals(true, p.positions().get(1).stale());
    }

    @Test
    void someoneElsesAccountIsReportedAsNotFound() {
        server.expect(requestTo("http://api.test/api/accounts/9/portfolio"))
                .andRespond(withStatus(HttpStatus.NOT_FOUND).contentType(MediaType.APPLICATION_PROBLEM_JSON)
                        .body("{\"status\":404,\"detail\":\"Account not found: 9\"}"));

        ApiException ex = assertThrows(ApiException.class, () -> client.portfolio(AUTH, 9L));

        assertEquals(ApiException.Kind.NOT_FOUND, ex.kind());
        assertEquals("Account not found: 9", ex.getMessage());
    }
}