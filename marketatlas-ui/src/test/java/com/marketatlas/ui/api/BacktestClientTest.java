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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.jsonPath;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

class BacktestClientTest {

    private static final String AUTH = "Basic dGVzdDp0ZXN0LXBhc3M=";
    private static final String BODY = "{\"assetId\":7,\"assetName\":\"Gold Spot\",\"currency\":\"USD\","
            + "\"strategy\":\"SMA_CROSSOVER\",\"firstBarDate\":\"2024-01-02\",\"lastBarDate\":\"2024-06-28\","
            + "\"barsUsed\":125,\"skippedBars\":0,\"initialCash\":10000,\"unknownField\":true,"
            + "\"metrics\":{\"totalReturnPct\":12.50,\"annualizedReturnPct\":26.0,\"maxDrawdownPct\":-5.2,"
            + "\"annualizedVolatilityPct\":14.0,\"trades\":4,\"commissionPaid\":4.00,\"exposurePct\":60.0,\"rejectedOrders\":0},"
            + "\"benchmark\":{\"totalReturnPct\":15.00,\"annualizedReturnPct\":31.0,\"maxDrawdownPct\":-7.0,"
            + "\"annualizedVolatilityPct\":15.0,\"trades\":1,\"commissionPaid\":1.00,\"exposurePct\":100.0,\"rejectedOrders\":0},"
            + "\"trades\":[{\"date\":\"2024-02-01\",\"side\":\"BUY\",\"quantity\":4,\"price\":2000.00,\"commission\":1.00}],"
            + "\"equityCurve\":[{\"date\":\"2024-01-02\",\"equity\":10000},{\"date\":\"2024-06-28\",\"equity\":11250}],"
            + "\"benchmarkEquityCurve\":[{\"date\":\"2024-01-02\",\"equity\":10000},{\"date\":\"2024-06-28\",\"equity\":11500}],"
            + "\"realTerms\":{\"baseMonth\":\"2024-01\",\"inflationPct\":1.2,\"realFinalEquity\":11100.5,"
            + "\"realTotalReturnPct\":11.0,\"benchmarkRealFinalEquity\":11360.0,\"benchmarkRealTotalReturnPct\":13.6,"
            + "\"cpiEstimated\":true},\"realTermsNote\":null}";

    private MockRestServiceServer server;
    private ApiClient client;

    @BeforeEach
    void setUp() {
        RestClient.Builder builder = RestClient.builder().baseUrl("http://api.test");
        server = MockRestServiceServer.bindTo(builder).build();
        client = new ApiClient(builder.build());
    }

    private static BacktestParams params() {
        return new BacktestParams(LocalDate.of(2024, 1, 1), LocalDate.of(2024, 6, 30), "SMA_CROSSOVER",
                20, 50, new BigDecimal("10000"), null, 5, true);
    }

    @Test
    void sendsAuthHeaderAndJsonAndParsesTheResult() {
        server.expect(requestTo("http://api.test/api/assets/7/backtest"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(header(HttpHeaders.AUTHORIZATION, AUTH))
                .andExpect(jsonPath("$.startDate").value("2024-01-01"))
                .andExpect(jsonPath("$.strategy").value("SMA_CROSSOVER"))
                .andExpect(jsonPath("$.shortWindow").value(20))
                .andExpect(jsonPath("$.initialCash").value(10000))
                .andExpect(jsonPath("$.includeEquityCurve").value(true))
                .andRespond(withSuccess(BODY, MediaType.APPLICATION_JSON));

        BacktestResult r = client.backtest(7L, AUTH, params());

        assertEquals("Gold Spot", r.assetName());
        assertEquals(new BigDecimal("12.50"), r.metrics().totalReturnPct());
        assertEquals(1, r.trades().size());
        assertEquals("BUY", r.trades().get(0).side());
        assertEquals(2, r.equityCurve().size());
        assertEquals(new BigDecimal("11500"), r.benchmarkEquityCurve().get(1).equity());
        assertEquals("2024-01", r.realTerms().baseMonth());
        assertEquals(true, r.realTerms().cpiEstimated());
        server.verify();
    }

    @Test
    void rateLimitKeepsRetryAfter() {
        server.expect(requestTo("http://api.test/api/assets/7/backtest"))
                .andRespond(withStatus(HttpStatus.TOO_MANY_REQUESTS).header(HttpHeaders.RETRY_AFTER, "42"));

        ApiException ex = assertThrows(ApiException.class, () -> client.backtest(7L, AUTH, params()));

        assertEquals(ApiException.Kind.RATE_LIMITED, ex.kind());
        assertEquals(42L, ex.retryAfterSeconds());
    }

    @Test
    void notFoundCarriesTheApiDetail() {
        server.expect(requestTo("http://api.test/api/assets/9/backtest"))
                .andRespond(withStatus(HttpStatus.NOT_FOUND).contentType(MediaType.APPLICATION_PROBLEM_JSON)
                        .body("{\"status\":404,\"detail\":\"Asset not found: 9\"}"));

        ApiException ex = assertThrows(ApiException.class, () -> client.backtest(9L, AUTH, params()));

        assertEquals(ApiException.Kind.NOT_FOUND, ex.kind());
        assertEquals("Asset not found: 9", ex.getMessage());
    }
}