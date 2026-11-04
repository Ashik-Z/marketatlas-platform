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
import static org.springframework.test.web.client.match.MockRestRequestMatchers.headerDoesNotExist;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

class DcaClientTest {

    private static final String BODY = "{\"assetId\":7,\"assetName\":\"Gold Spot\",\"currency\":\"USD\","
            + "\"cpiSeries\":\"CPIAUCNS\",\"attribution\":\"FRED\",\"baseMonth\":\"2001-03-01\","
            + "\"latestCpiMonth\":\"2001-03-01\",\"amountPerPurchase\":100,\"purchaseCount\":2,"
            + "\"investedNominal\":200.00,\"investedReal\":221.00,\"units\":3.65289256,"
            + "\"valuationDate\":\"2001-03-10\",\"valuationClose\":60.5,\"valueNominal\":221.00,"
            + "\"valueReal\":221.00,\"nominalGain\":21.00,\"realGain\":0.00,\"nominalGainPct\":10.5,"
            + "\"realGainPct\":0.0,\"extra\":1,\"purchases\":["
            + "{\"date\":\"2001-01-15\",\"close\":50.0,\"units\":2.0,\"amount\":100,\"realAmount\":121.00,"
            + "\"cpiEstimated\":false},"
            + "{\"date\":\"2001-03-10\",\"close\":60.5,\"units\":1.65289256,\"amount\":100,\"realAmount\":100.00,"
            + "\"cpiEstimated\":true}]}";

    private MockRestServiceServer server;
    private ApiClient client;

    @BeforeEach
    void setUp() {
        RestClient.Builder builder = RestClient.builder().baseUrl("http://api.test");
        server = MockRestServiceServer.bindTo(builder).build();
        client = new ApiClient(builder.build());
    }

    @Test
    void parsesTheResultAndSendsDatesAndAmountWithoutLogin() {
        server.expect(requestTo("http://api.test/api/assets/7/dca?startDate=2001-01-01&endDate=2001-12-31&amount=100.50"))
                .andExpect(method(HttpMethod.GET))
                .andExpect(headerDoesNotExist(HttpHeaders.AUTHORIZATION))
                .andRespond(withSuccess(BODY, MediaType.APPLICATION_JSON));

        DcaResult r = client.dca(7L, LocalDate.of(2001, 1, 1), LocalDate.of(2001, 12, 31), new BigDecimal("100.50"));

        assertEquals("Gold Spot", r.assetName());
        assertEquals(2, r.purchaseCount());
        assertEquals(new BigDecimal("200.00"), r.investedNominal());
        assertEquals(new BigDecimal("221.00"), r.valueReal());
        assertEquals(new BigDecimal("0.00"), r.realGain());
        assertEquals(2, r.purchases().size());
        assertEquals(LocalDate.of(2001, 3, 10), r.purchases().get(1).date());
        assertEquals(true, r.purchases().get(1).cpiEstimated());
        server.verify();
    }

    @Test
    void badRequestMapsToItsKind() {
        server.expect(requestTo("http://api.test/api/assets/7/dca?startDate=2001-01-01&endDate=2001-12-31&amount=0"))
                .andRespond(withStatus(HttpStatus.BAD_REQUEST).contentType(MediaType.APPLICATION_PROBLEM_JSON)
                        .body("{\"detail\":\"amount must be greater than zero\"}"));
        ApiException bad = assertThrows(ApiException.class,
                () -> client.dca(7L, LocalDate.of(2001, 1, 1), LocalDate.of(2001, 12, 31), BigDecimal.ZERO));
        assertEquals(ApiException.Kind.INVALID_INPUT, bad.kind());
    }

    @Test
    void notFoundMapsToItsKind() {
        server.expect(requestTo("http://api.test/api/assets/9/dca?startDate=2001-01-01&endDate=2001-12-31&amount=100"))
                .andRespond(withStatus(HttpStatus.NOT_FOUND));
        ApiException missing = assertThrows(ApiException.class,
                () -> client.dca(9L, LocalDate.of(2001, 1, 1), LocalDate.of(2001, 12, 31), new BigDecimal("100")));
        assertEquals(ApiException.Kind.NOT_FOUND, missing.kind());
    }
}
