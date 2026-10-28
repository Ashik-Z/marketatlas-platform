package com.marketatlas.trading.controller;

import com.jayway.jsonpath.JsonPath;
import com.marketatlas.market.TestPostgresConfiguration;
import com.marketatlas.market.entity.Asset;
import com.marketatlas.market.entity.MarketData;
import com.marketatlas.market.repository.AssetRepository;
import com.marketatlas.market.repository.MarketDataRepository;
import com.marketatlas.trading.model.Trade;
import com.marketatlas.trading.model.TradeSide;
import com.marketatlas.trading.repository.AccountRepository;
import com.marketatlas.trading.repository.TradeRepository;
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

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Base64;
import java.util.UUID;

import static org.hamcrest.Matchers.nullValue;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Import(TestPostgresConfiguration.class)
@Transactional
class PortfolioApiIntegrationTest {

    private static final String AUTH = "Basic " + Base64.getEncoder()
            .encodeToString("admin:test-only-password".getBytes(StandardCharsets.UTF_8));

    @Autowired private MockMvc mockMvc;
    @Autowired private AssetRepository assetRepository;
    @Autowired private MarketDataRepository marketDataRepository;
    @Autowired private AccountRepository accountRepository;
    @Autowired private TradeRepository tradeRepository;
    @Autowired private Clock clock;

    private static MockHttpServletRequestBuilder post(String url, String json) {
        MockHttpServletRequestBuilder request = MockMvcRequestBuilders.post(url)
                .header("Authorization", AUTH)
                .contentType(MediaType.APPLICATION_JSON).content(json);
        return url.endsWith("/orders")
                ? request.header("Idempotency-Key", UUID.randomUUID().toString())
                : request;
    }

    private static MockHttpServletRequestBuilder get(String url) {
        return MockMvcRequestBuilders.get(url).header("Authorization", AUTH);
    }

    private int idOf(MockHttpServletRequestBuilder request) throws Exception {
        String body = mockMvc.perform(request).andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return JsonPath.read(body, "$.id");
    }

    private int account() throws Exception {
        return idOf(post("/api/accounts",
                "{\"name\": \"Demo\", \"currency\": \"USD\", \"startingBalance\": 10000}"));
    }

    private int commodity(String name) throws Exception {
        return idOf(post("/api/assets", "{\"name\": \"" + name
                + "\", \"assetClass\": \"COMMODITY\", \"currency\": \"USD\"}"));
    }

    private void bar(int assetId, LocalDate date, double close) {
        Asset asset = assetRepository.findById((long) assetId).orElseThrow();
        marketDataRepository.save(new MarketData(close, close, close, close, 1000.0, asset, date));
    }

    private void buy(int accountId, int assetId, String quantity) throws Exception {
        mockMvc.perform(post("/api/accounts/" + accountId + "/orders",
                        "{\"assetId\": " + assetId + ", \"side\": \"BUY\", \"quantity\": " + quantity + "}"))
                .andExpect(status().isCreated());
    }

    @Test
    void shouldShowAnUntradedAccountAtItsStartingValue() throws Exception {
        int accountId = account();

        mockMvc.perform(get("/api/accounts/" + accountId + "/portfolio"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.cashBalance").value(10000.0))
                .andExpect(jsonPath("$.positionsValue").value(0.0))
                .andExpect(jsonPath("$.totalValue").value(10000.0))
                .andExpect(jsonPath("$.profitLoss").value(0.0))
                .andExpect(jsonPath("$.valuationComplete").value(true))
                .andExpect(jsonPath("$.positions.length()").value(0));
    }

    @Test
    void shouldValuePositionsAtTheLatestCloseAndTrackProfitAndLoss() throws Exception {
        int accountId = account();
        int goldId = commodity("Gold");
        LocalDate today = LocalDate.now(clock);
        bar(goldId, today.minusDays(1), 2000.0);

        buy(accountId, goldId, "2"); // 4000 + 1 commission -> cash 5999

        // priced at 2000: positions 4000, total 9999, the commission is the only loss
        mockMvc.perform(get("/api/accounts/" + accountId + "/portfolio"))
                .andExpect(jsonPath("$.cashBalance").value(5999.0))
                .andExpect(jsonPath("$.positionsValue").value(4000.0))
                .andExpect(jsonPath("$.totalValue").value(9999.0))
                .andExpect(jsonPath("$.profitLoss").value(-1.0))
                .andExpect(jsonPath("$.positions[0].quantity").value(2.0))
                .andExpect(jsonPath("$.positions[0].stale").value(false));

        // gold moves to 2100: positions 4200, total 10199, profit 199
        bar(goldId, today, 2100.0);
        mockMvc.perform(get("/api/accounts/" + accountId + "/portfolio"))
                .andExpect(jsonPath("$.positions[0].latestPrice").value(2100.0))
                .andExpect(jsonPath("$.positions[0].priceDate").value(today.toString()))
                .andExpect(jsonPath("$.positions[0].marketValue").value(4200.0))
                .andExpect(jsonPath("$.totalValue").value(10199.0))
                .andExpect(jsonPath("$.profitLoss").value(199.0));
    }

    @Test
    void shouldDropFullySoldPositions() throws Exception {
        int accountId = account();
        int goldId = commodity("Gold");
        bar(goldId, LocalDate.now(clock), 2000.0);
        buy(accountId, goldId, "1");

        mockMvc.perform(post("/api/accounts/" + accountId + "/orders",
                        "{\"assetId\": " + goldId + ", \"side\": \"SELL\", \"quantity\": 1}"))
                .andExpect(status().isCreated());

        mockMvc.perform(get("/api/accounts/" + accountId + "/portfolio"))
                .andExpect(jsonPath("$.positions.length()").value(0))
                .andExpect(jsonPath("$.totalValue").value(9998.0)); // two commissions lost
    }

    @Test
    void shouldFlagAStalePrice() throws Exception {
        int accountId = account();
        int goldId = commodity("Gold");
        LocalDate old = LocalDate.now(clock).minusDays(10);
        bar(goldId, old, 2000.0);
        saveTradeDirectly(accountId, goldId);

        mockMvc.perform(get("/api/accounts/" + accountId + "/portfolio"))
                .andExpect(jsonPath("$.positions[0].stale").value(true))
                .andExpect(jsonPath("$.positions[0].priceDate").value(old.toString()))
                .andExpect(jsonPath("$.valuationComplete").value(true));
    }

    @Test
    void shouldReportAnIncompleteValuationInsteadOfGuessingAPrice() throws Exception {
        int accountId = account();
        int silverId = commodity("Silver"); // no price bars stored at all
        saveTradeDirectly(accountId, silverId);

        mockMvc.perform(get("/api/accounts/" + accountId + "/portfolio"))
                .andExpect(jsonPath("$.valuationComplete").value(false))
                .andExpect(jsonPath("$.positions[0].latestPrice").value(nullValue()))
                .andExpect(jsonPath("$.positions[0].marketValue").value(nullValue()))
                .andExpect(jsonPath("$.positions[0].stale").value(true));
    }

    @Test
    void shouldRequireAdminAndReturn404ForUnknownAccount() throws Exception {
        int accountId = account();

        mockMvc.perform(MockMvcRequestBuilders.get("/api/accounts/" + accountId + "/portfolio"))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(get("/api/accounts/999999/portfolio"))
                .andExpect(status().isNotFound());
    }

    private void saveTradeDirectly(int accountId, int assetId) {
        var account = accountRepository.findById((long) accountId).orElseThrow();
        Asset asset = assetRepository.findById((long) assetId).orElseThrow();
        tradeRepository.save(new Trade(account, asset, TradeSide.BUY, BigDecimal.ONE,
                new BigDecimal("2000"), BigDecimal.ONE, LocalDate.now(clock), Instant.now(clock),
                "direct-" + UUID.randomUUID()));
    }
}