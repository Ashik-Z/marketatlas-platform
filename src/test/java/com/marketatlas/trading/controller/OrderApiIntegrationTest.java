package com.marketatlas.trading.controller;

import com.jayway.jsonpath.JsonPath;
import com.marketatlas.market.TestPostgresConfiguration;
import com.marketatlas.market.entity.Asset;
import com.marketatlas.market.entity.MarketData;
import com.marketatlas.market.repository.AssetRepository;
import com.marketatlas.market.repository.MarketDataRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.LocalDate;
import java.util.Base64;

import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Import(TestPostgresConfiguration.class)
@Transactional
class OrderApiIntegrationTest {

    private static final String AUTH = "Basic " + Base64.getEncoder()
            .encodeToString("admin:test-only-password".getBytes(StandardCharsets.UTF_8));

    @Autowired private MockMvc mockMvc;
    @Autowired private AssetRepository assetRepository;
    @Autowired private MarketDataRepository marketDataRepository;
    @Autowired private Clock clock;

    private static MockHttpServletRequestBuilder post(String url, String json) {
        return MockMvcRequestBuilders.post(url).header("Authorization", AUTH)
                .contentType(MediaType.APPLICATION_JSON).content(json);
    }

    private static MockHttpServletRequestBuilder get(String url) {
        return MockMvcRequestBuilders.get(url).header("Authorization", AUTH);
    }

    private int idOf(MockHttpServletRequestBuilder request) throws Exception {
        String body = mockMvc.perform(request).andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return JsonPath.read(body, "$.id");
    }

    private int account(String currency, String balance) throws Exception {
        return idOf(post("/api/accounts", "{\"name\": \"Demo\", \"currency\": \"" + currency
                + "\", \"startingBalance\": " + balance + "}"));
    }

    private int gold() throws Exception {
        return idOf(post("/api/assets",
                "{\"name\": \"Gold\", \"assetClass\": \"COMMODITY\", \"currency\": \"USD\", \"unit\": \"troy ounce\"}"));
    }

    private int equity() throws Exception {
        int issuerId = idOf(post("/api/issuers", "{\"name\": \"Apple Inc.\", \"country\": \"United States\"}"));
        return idOf(post("/api/assets", "{\"name\": \"Apple\", \"assetClass\": \"EQUITY\", \"issuerId\": "
                + issuerId + ", \"symbol\": \"AAPL\", \"currency\": \"USD\"}"));
    }

    private void bar(int assetId, LocalDate date, double close) {
        Asset asset = assetRepository.findById((long) assetId).orElseThrow();
        marketDataRepository.save(new MarketData(close, close, close, close, 1000.0, asset, date));
    }

    private String order(int assetId, String side, String quantity) {
        return "{\"assetId\": " + assetId + ", \"side\": \"" + side + "\", \"quantity\": " + quantity + "}";
    }

    private void assertRejected(int accountId, String json, String detailPart) throws Exception {
        mockMvc.perform(post("/api/accounts/" + accountId + "/orders", json))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.detail").value(containsString(detailPart)));
    }

    @Test
    void shouldBuyThenSellWithExactCashArithmetic() throws Exception {
        int accountId = account("USD", "10000");
        int goldId = gold();
        LocalDate today = LocalDate.now(clock);
        bar(goldId, today, 2000.50);

        // buy 2 at 2000.50 = 4001.00 + 1.00 commission -> cash 5998.00
        mockMvc.perform(post("/api/accounts/" + accountId + "/orders", order(goldId, "BUY", "2")))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.side").value("BUY"))
                .andExpect(jsonPath("$.price").value(2000.5))
                .andExpect(jsonPath("$.commission").value(1.0))
                .andExpect(jsonPath("$.priceDate").value(today.toString()));
        mockMvc.perform(get("/api/accounts/" + accountId))
                .andExpect(jsonPath("$.cashBalance").value(5998.0));

        // sell 1 at 2000.50 = 2000.50 - 1.00 -> cash 7997.50
        mockMvc.perform(post("/api/accounts/" + accountId + "/orders", order(goldId, "SELL", "1")))
                .andExpect(status().isCreated());
        mockMvc.perform(get("/api/accounts/" + accountId))
                .andExpect(jsonPath("$.cashBalance").value(7997.5));

        // only 1 ounce left; selling 5 is rejected and cash is unchanged
        assertRejected(accountId, order(goldId, "SELL", "5"), "Insufficient position");
        mockMvc.perform(get("/api/accounts/" + accountId))
                .andExpect(jsonPath("$.cashBalance").value(7997.5));
    }

    @Test
    void shouldAllowFractionalCommodityUnits() throws Exception {
        int accountId = account("USD", "10000");
        int goldId = gold();
        bar(goldId, LocalDate.now(clock), 2000.0);

        mockMvc.perform(post("/api/accounts/" + accountId + "/orders", order(goldId, "BUY", "0.5")))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.quantity").value(0.5));
    }

    @Test
    void shouldRejectFractionalEquityShares() throws Exception {
        int accountId = account("USD", "10000");
        int equityId = equity();
        bar(equityId, LocalDate.now(clock), 200.0);

        assertRejected(accountId, order(equityId, "BUY", "1.5"), "whole shares");
        mockMvc.perform(post("/api/accounts/" + accountId + "/orders", order(equityId, "BUY", "2")))
                .andExpect(status().isCreated());
    }

    @Test
    void shouldRejectInsufficientCash() throws Exception {
        int accountId = account("USD", "1000");
        int goldId = gold();
        bar(goldId, LocalDate.now(clock), 2000.0);

        assertRejected(accountId, order(goldId, "BUY", "1"), "Insufficient cash");
        mockMvc.perform(get("/api/accounts/" + accountId))
                .andExpect(jsonPath("$.cashBalance").value(1000.0));
    }

    @Test
    void shouldRejectCurrencyMismatch() throws Exception {
        int accountId = account("EUR", "10000");
        int goldId = gold();
        bar(goldId, LocalDate.now(clock), 2000.0);

        assertRejected(accountId, order(goldId, "BUY", "1"), "does not match account currency");
    }

    @Test
    void shouldRejectMissingAndStalePrices() throws Exception {
        int accountId = account("USD", "10000");
        int goldId = gold();

        assertRejected(accountId, order(goldId, "BUY", "1"), "No price data");

        bar(goldId, LocalDate.now(clock).minusDays(10), 2000.0);
        assertRejected(accountId, order(goldId, "BUY", "1"), "older than");
    }

    @Test
    void shouldAcceptAWeekendOldPrice() throws Exception {
        int accountId = account("USD", "10000");
        int goldId = gold();
        bar(goldId, LocalDate.now(clock).minusDays(3), 2000.0);

        mockMvc.perform(post("/api/accounts/" + accountId + "/orders", order(goldId, "BUY", "1")))
                .andExpect(status().isCreated());
    }

    @Test
    void shouldReturn404ForUnknownAccountOrAssetAnd400ForBadQuantity() throws Exception {
        int accountId = account("USD", "10000");
        int goldId = gold();

        mockMvc.perform(post("/api/accounts/999999/orders", order(goldId, "BUY", "1")))
                .andExpect(status().isNotFound());
        mockMvc.perform(post("/api/accounts/" + accountId + "/orders", order(999999, "BUY", "1")))
                .andExpect(status().isNotFound());
        mockMvc.perform(post("/api/accounts/" + accountId + "/orders", order(goldId, "BUY", "0")))
                .andExpect(status().isBadRequest());
        mockMvc.perform(post("/api/accounts/" + accountId + "/orders", order(goldId, "HOLD", "1")))
                .andExpect(status().isBadRequest());
    }
}