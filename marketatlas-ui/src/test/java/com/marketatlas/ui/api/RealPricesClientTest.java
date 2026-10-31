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
import java.time.YearMonth;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.headerDoesNotExist;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

class RealPricesClientTest {

    private static final String BODY = "{\"assetId\":7,\"assetName\":\"Gold Spot\",\"currency\":\"USD\","
            + "\"cpiSeries\":\"CPIAUCNS\",\"cpiName\":\"CPI\",\"attribution\":\"FRED\","
            + "\"baseMonth\":\"2024-01-01\",\"baseCpi\":308.417,\"latestCpiMonth\":\"2024-02-01\","
            + "\"nominalReturnPct\":1.50,\"realReturnPct\":1.10,\"extra\":1,"
            + "\"points\":[{\"date\":\"2024-01-02\",\"nominalClose\":2000.5,\"realClose\":2000.5,"
            + "\"cpiMonth\":\"2024-01-01\",\"cpiEstimated\":false},"
            + "{\"date\":\"2024-03-01\",\"nominalClose\":2030.5,\"realClose\":2022.0,"
            + "\"cpiMonth\":\"2024-02-01\",\"cpiEstimated\":true}]}";

    private MockRestServiceServer server;
    private ApiClient client;

    @BeforeEach
    void setUp() {
        RestClient.Builder builder = RestClient.builder().baseUrl("http://api.test");
        server = MockRestServiceServer.bindTo(builder).build();
        client = new ApiClient(builder.build());
    }

    @Test
    void parsesPointsAndSendsDatesAndBaseMonth() {
        server.expect(requestTo("http://api.test/api/assets/7/real-prices?startDate=2024-01-01&endDate=2024-03-01&baseMonth=2024-01"))
                .andExpect(method(HttpMethod.GET))
                .andExpect(headerDoesNotExist(HttpHeaders.AUTHORIZATION))
                .andRespond(withSuccess(BODY, MediaType.APPLICATION_JSON));

        RealPrices r = client.realPrices(7L, LocalDate.of(2024, 1, 1), LocalDate.of(2024, 3, 1), YearMonth.of(2024, 1));

        assertEquals("Gold Spot", r.assetName());
        assertEquals(LocalDate.of(2024, 1, 1), r.baseMonth());
        assertEquals(new BigDecimal("1.10"), r.realReturnPct());
        assertEquals(2, r.points().size());
        assertEquals(LocalDate.of(2024, 3, 1), r.points().get(1).date());
        assertEquals(true, r.points().get(1).cpiEstimated());
        server.verify();
    }

    @Test
    void omitsBaseMonthWhenNotGiven() {
        server.expect(requestTo("http://api.test/api/assets/7/real-prices?startDate=2024-01-01&endDate=2024-03-01"))
                .andRespond(withSuccess(BODY, MediaType.APPLICATION_JSON));

        client.realPrices(7L, LocalDate.of(2024, 1, 1), LocalDate.of(2024, 3, 1), null);

        server.verify();
    }

    @Test
    void notFoundCarriesTheApiDetail() {
        server.expect(requestTo("http://api.test/api/assets/9/real-prices?startDate=2024-01-01&endDate=2024-03-01"))
                .andRespond(withStatus(HttpStatus.NOT_FOUND).contentType(MediaType.APPLICATION_PROBLEM_JSON)
                        .body("{\"status\":404,\"detail\":\"Asset not found: 9\"}"));

        ApiException ex = assertThrows(ApiException.class,
                () -> client.realPrices(9L, LocalDate.of(2024, 1, 1), LocalDate.of(2024, 3, 1), null));

        assertEquals(ApiException.Kind.NOT_FOUND, ex.kind());
        assertEquals("Asset not found: 9", ex.getMessage());
    }

    @Test
    void badRequestCarriesTheApiDetail() {
        server.expect(requestTo("http://api.test/api/assets/7/real-prices?startDate=2024-01-01&endDate=2024-03-01"))
                .andRespond(withStatus(HttpStatus.BAD_REQUEST).contentType(MediaType.APPLICATION_PROBLEM_JSON)
                        .body("{\"status\":400,\"detail\":\"Real prices are only available for USD assets\"}"));

        ApiException ex = assertThrows(ApiException.class,
                () -> client.realPrices(7L, LocalDate.of(2024, 1, 1), LocalDate.of(2024, 3, 1), null));

        assertEquals(ApiException.Kind.INVALID_INPUT, ex.kind());
        assertEquals("Real prices are only available for USD assets", ex.getMessage());
    }
}