package com.marketatlas.backtest;

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

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = "marketatlas.rate-limit.backtests-per-minute=2")
@AutoConfigureMockMvc
@Import(TestPostgresConfiguration.class)
@Transactional
class BacktestRateLimitIntegrationTest {

    private static final String PASSWORD = "a-long-test-password";
    private static final String JSON = "{\"startDate\":\"2024-01-01\",\"endDate\":\"2024-01-31\","
            + "\"strategy\":\"BUY_AND_HOLD\",\"initialCash\":1000}";

    @Autowired MockMvc mockMvc;

    private static String basic(String user) {
        String raw = user + ":" + PASSWORD;
        return "Basic " + Base64.getEncoder().encodeToString(raw.getBytes(StandardCharsets.UTF_8));
    }

    private void signUp(String username) throws Exception {
        mockMvc.perform(post("/api/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"" + username + "\",\"password\":\"" + PASSWORD + "\"}"))
                .andExpect(status().isCreated());
    }

    private int call(String user) throws Exception {
        return mockMvc.perform(post("/api/assets/999999999/backtest")
                        .header("Authorization", basic(user))
                        .contentType(MediaType.APPLICATION_JSON).content(JSON))
                .andReturn().getResponse().getStatus();
    }

    @Test
    void thirdBacktestInAMinuteIs429WithRetryAfterAndOtherUsersAreNotAffected() throws Exception {
        signUp("erin");
        signUp("frank");

        org.junit.jupiter.api.Assertions.assertEquals(404, call("erin"));
        org.junit.jupiter.api.Assertions.assertEquals(404, call("erin"));

        mockMvc.perform(post("/api/assets/999999999/backtest")
                        .header("Authorization", basic("erin"))
                        .contentType(MediaType.APPLICATION_JSON).content(JSON))
                .andExpect(status().isTooManyRequests())
                .andExpect(header().exists("Retry-After"))
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON));

        org.junit.jupiter.api.Assertions.assertEquals(404, call("frank"));
    }
}