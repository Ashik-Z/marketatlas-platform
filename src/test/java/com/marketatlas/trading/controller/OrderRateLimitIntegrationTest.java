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
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = "marketatlas.rate-limit.orders-per-minute=3")
@AutoConfigureMockMvc
@Import(TestPostgresConfiguration.class)
@Transactional
class OrderRateLimitIntegrationTest {

    private static final String PASSWORD = "a-long-test-password";

    @Autowired
    MockMvc mockMvc;

    private static String basic(String user) {
        String raw = user + ":" + PASSWORD;
        return "Basic " + Base64.getEncoder().encodeToString(raw.getBytes(StandardCharsets.UTF_8));
    }

    /** Signs the user up and creates one account for them; returns the account id. */
    private long userWithAccount(String username) throws Exception {
        mockMvc.perform(post("/api/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"" + username + "\",\"password\":\"" + PASSWORD + "\"}"))
                .andExpect(status().isCreated());
        String body = mockMvc.perform(post("/api/accounts")
                        .header("Authorization", basic(username))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Acct\",\"currency\":\"USD\",\"startingBalance\":1000}"))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return ((Number) JsonPath.read(body, "$.id")).longValue();
    }

    private int placeOrder(String username, long accountId, String key) throws Exception {
        return mockMvc.perform(post("/api/accounts/" + accountId + "/orders")
                        .header("Authorization", basic(username))
                        .header("Idempotency-Key", key)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"assetId\":999999,\"side\":\"BUY\",\"quantity\":1}"))
                .andReturn().getResponse().getStatus();
    }

    @Test
    void fourthOrderWithinTheWindowGets429() throws Exception {
        long account = userWithAccount("ol.one");

        for (int i = 1; i <= 3; i++) {
            assertNotEquals(429, placeOrder("ol.one", account, "k" + i));
        }

        mockMvc.perform(post("/api/accounts/" + account + "/orders")
                        .header("Authorization", basic("ol.one"))
                        .header("Idempotency-Key", "k4")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"assetId\":999999,\"side\":\"BUY\",\"quantity\":1}"))
                .andExpect(status().isTooManyRequests())
                .andExpect(header().exists("Retry-After"))
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON));
    }

    @Test
    void limitIsPerUser() throws Exception {
        long a = userWithAccount("ol.a");
        long b = userWithAccount("ol.b");

        for (int i = 1; i <= 3; i++) {
            placeOrder("ol.a", a, "a" + i);
        }
        assertEquals(429, placeOrder("ol.a", a, "a4"));

        assertNotEquals(429, placeOrder("ol.b", b, "b1"));
    }

    @Test
    void requestsForSomeoneElsesAccountNeitherSucceedNorUseUpBudget() throws Exception {
        long owner = userWithAccount("ol.owner");
        long other = userWithAccount("ol.other");

        for (int i = 1; i <= 5; i++) {
            assertEquals(404, placeOrder("ol.other", owner, "x" + i));
        }

        // The 404s came from the ownership check, which runs before the limiter.
        assertNotEquals(429, placeOrder("ol.other", other, "y1"));
    }
}