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
import java.util.UUID;

import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Import(TestPostgresConfiguration.class)
@Transactional
class TradeHistoryApiIntegrationTest {

    private static final String AUTH = "Basic " + Base64.getEncoder()
            .encodeToString("admin:test-only-password".getBytes(StandardCharsets.UTF_8));

    @Autowired private MockMvc mockMvc;
    @Autowired private AssetRepository assetRepository;
    @Autowired private MarketDataRepository marketDataRepository;
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
                "{\"name\": \"Demo\", \"currency\": \"USD\", \"startingBalance\": 100000}"));
    }

    private int gold() throws Exception {
        int goldId = idOf(post("/api/assets",
                "{\"name\": \"Gold\", \"assetClass\": \"COMMODITY\", \"currency\": \"USD\"}"));
        Asset asset = assetRepository.findById((long) goldId).orElseThrow();
        marketDataRepository.save(new MarketData(2000.0, 2000.0, 2000.0, 2000.0, 1000.0,
                asset, LocalDate.now(clock)));
        return goldId;
    }

    private void buy(int accountId, int assetId, String quantity) throws Exception {
        mockMvc.perform(post("/api/accounts/" + accountId + "/orders",
                        "{\"assetId\": " + assetId + ", \"side\": \"BUY\", \"quantity\": " + quantity + "}"))
                .andExpect(status().isCreated());
    }

    @Test
    void shouldReturnAnEmptyPageForAnAccountWithoutTrades() throws Exception {
        int accountId = account();

        mockMvc.perform(get("/api/accounts/" + accountId + "/trades"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(0))
                .andExpect(jsonPath("$.page").value(0))
                .andExpect(jsonPath("$.size").value(50))
                .andExpect(jsonPath("$.totalElements").value(0));
    }

    @Test
    void shouldListNewestFirstAndPage() throws Exception {
        int accountId = account();
        int goldId = gold();
        buy(accountId, goldId, "1");
        buy(accountId, goldId, "2");
        buy(accountId, goldId, "3");

        mockMvc.perform(get("/api/accounts/" + accountId + "/trades"))
                .andExpect(jsonPath("$.totalElements").value(3))
                .andExpect(jsonPath("$.content[0].quantity").value(3.0))
                .andExpect(jsonPath("$.content[1].quantity").value(2.0))
                .andExpect(jsonPath("$.content[2].quantity").value(1.0));

        mockMvc.perform(get("/api/accounts/" + accountId + "/trades?page=0&size=2"))
                .andExpect(jsonPath("$.content.length()").value(2))
                .andExpect(jsonPath("$.content[0].quantity").value(3.0))
                .andExpect(jsonPath("$.totalPages").value(2));

        mockMvc.perform(get("/api/accounts/" + accountId + "/trades?page=1&size=2"))
                .andExpect(jsonPath("$.content.length()").value(1))
                .andExpect(jsonPath("$.content[0].quantity").value(1.0));
    }

    @Test
    void shouldOnlyShowTheRequestedAccountsTrades() throws Exception {
        int first = account();
        int second = account();
        int goldId = gold();
        buy(first, goldId, "1");

        mockMvc.perform(get("/api/accounts/" + first + "/trades"))
                .andExpect(jsonPath("$.totalElements").value(1));
        mockMvc.perform(get("/api/accounts/" + second + "/trades"))
                .andExpect(jsonPath("$.totalElements").value(0));
    }

    @Test
    void shouldRejectOutOfRangePaging() throws Exception {
        int accountId = account();

        mockMvc.perform(get("/api/accounts/" + accountId + "/trades?size=0"))
                .andExpect(status().isBadRequest());
        mockMvc.perform(get("/api/accounts/" + accountId + "/trades?size=201"))
                .andExpect(status().isBadRequest());
        mockMvc.perform(get("/api/accounts/" + accountId + "/trades?page=-1"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void shouldRequireAdminAndReturn404ForUnknownAccount() throws Exception {
        int accountId = account();

        mockMvc.perform(MockMvcRequestBuilders.get("/api/accounts/" + accountId + "/trades"))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(get("/api/accounts/999999/trades"))
                .andExpect(status().isNotFound());
    }
}