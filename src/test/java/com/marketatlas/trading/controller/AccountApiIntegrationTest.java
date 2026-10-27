package com.marketatlas.trading.controller;

import com.jayway.jsonpath.JsonPath;
import com.marketatlas.market.TestPostgresConfiguration;
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
import java.util.Base64;

import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Import(TestPostgresConfiguration.class)
@Transactional
class AccountApiIntegrationTest {

    private static final String AUTH = "Basic " + Base64.getEncoder()
            .encodeToString("admin:test-only-password".getBytes(StandardCharsets.UTF_8));

    @Autowired
    private MockMvc mockMvc;

    private static MockHttpServletRequestBuilder post(String url, String json) {
        return MockMvcRequestBuilders.post(url)
                .header("Authorization", AUTH)
                .contentType(MediaType.APPLICATION_JSON)
                .content(json);
    }

    private static MockHttpServletRequestBuilder get(String url) {
        return MockMvcRequestBuilders.get(url).header("Authorization", AUTH);
    }

    private static String accountJson(String name, String currency, String balance) {
        return "{\"name\": \"" + name + "\", \"currency\": \"" + currency
                + "\", \"startingBalance\": " + balance + "}";
    }

    @Test
    void shouldCreateAccountWithCashEqualToStartingBalanceAndReadItBack() throws Exception {
        String body = mockMvc.perform(post("/api/accounts", accountJson("  Demo  ", "USD", "10000.50")))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name").value("Demo"))
                .andExpect(jsonPath("$.currency").value("USD"))
                .andExpect(jsonPath("$.startingBalance").value(10000.5))
                .andExpect(jsonPath("$.cashBalance").value(10000.5))
                .andExpect(jsonPath("$.createdAt").exists())
                .andReturn().getResponse().getContentAsString();
        int id = JsonPath.read(body, "$.id");

        mockMvc.perform(get("/api/accounts/" + id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Demo"));

        mockMvc.perform(get("/api/accounts"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1));
    }

    @Test
    void shouldRejectInvalidInput() throws Exception {
        mockMvc.perform(post("/api/accounts", accountJson("Demo", "usd", "100")))
                .andExpect(status().isBadRequest());
        mockMvc.perform(post("/api/accounts", accountJson("Demo", "US", "100")))
                .andExpect(status().isBadRequest());
        mockMvc.perform(post("/api/accounts", accountJson("   ", "USD", "100")))
                .andExpect(status().isBadRequest());
        mockMvc.perform(post("/api/accounts", accountJson("Demo", "USD", "-1")))
                .andExpect(status().isBadRequest());
        mockMvc.perform(post("/api/accounts", accountJson("Demo", "USD", "1.1234567")))
                .andExpect(status().isBadRequest());
        mockMvc.perform(post("/api/accounts", "{\"name\": \"Demo\", \"currency\": \"USD\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void shouldReturn404ForUnknownAccount() throws Exception {
        mockMvc.perform(get("/api/accounts/999999"))
                .andExpect(status().isNotFound());
    }
}