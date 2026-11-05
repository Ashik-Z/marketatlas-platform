package com.marketatlas.trading.controller;

import com.jayway.jsonpath.JsonPath;
import com.marketatlas.market.TestPostgresConfiguration;
import org.junit.jupiter.api.BeforeEach;
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

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** The administrator operates the platform and may read every account, but never trades. */
@SpringBootTest
@AutoConfigureMockMvc
@Import(TestPostgresConfiguration.class)
@Transactional
class AdminReadOnlyAccountsTest {

    private static final String PASSWORD = "a-long-test-password";
    private static final String ACCOUNT = "{\"name\":\"Acct\",\"currency\":\"USD\",\"startingBalance\":1000}";

    @Autowired
    MockMvc mockMvc;

    private long aliceAccountId;

    private static String basic(String user, String password) {
        String raw = user + ":" + password;
        return "Basic " + Base64.getEncoder().encodeToString(raw.getBytes(StandardCharsets.UTF_8));
    }

    private static String admin() {
        return basic("admin", "test-only-password");
    }

    @BeforeEach
    void setUp() throws Exception {
        mockMvc.perform(post("/api/users").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"alice\",\"password\":\"" + PASSWORD + "\"}"))
                .andExpect(status().isCreated());
        String body = mockMvc.perform(post("/api/accounts").header("Authorization", basic("alice", PASSWORD))
                        .contentType(MediaType.APPLICATION_JSON).content(ACCOUNT))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        aliceAccountId = ((Number) JsonPath.read(body, "$.id")).longValue();
    }

    @Test
    void adminCannotCreateAnAccount() throws Exception {
        mockMvc.perform(post("/api/accounts").header("Authorization", admin())
                        .contentType(MediaType.APPLICATION_JSON).content(ACCOUNT))
                .andExpect(status().isForbidden());
    }

    @Test
    void adminCannotPlaceAnOrder() throws Exception {
        mockMvc.perform(post("/api/accounts/" + aliceAccountId + "/orders").header("Authorization", admin())
                        .header("Idempotency-Key", "k-1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"assetId\":1,\"side\":\"BUY\",\"quantity\":1}"))
                .andExpect(status().isForbidden());
    }

    @Test
    void adminCanStillReadAUsersAccountAndPortfolio() throws Exception {
        mockMvc.perform(get("/api/accounts/" + aliceAccountId).header("Authorization", admin()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Acct"));
        mockMvc.perform(get("/api/accounts").header("Authorization", admin()))
                .andExpect(status().isOk());
        mockMvc.perform(get("/api/accounts/" + aliceAccountId + "/portfolio").header("Authorization", admin()))
                .andExpect(status().isOk());
    }
}
