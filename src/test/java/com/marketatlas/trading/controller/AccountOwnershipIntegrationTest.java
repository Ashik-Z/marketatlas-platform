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

@SpringBootTest
@AutoConfigureMockMvc
@Import(TestPostgresConfiguration.class)
@Transactional
class AccountOwnershipIntegrationTest {

    private static final String PASSWORD = "a-long-test-password";

    @Autowired
    MockMvc mockMvc;

    private long aliceAccountId;

    private static String basic(String user, String password) {
        String raw = user + ":" + password;
        return "Basic " + Base64.getEncoder().encodeToString(raw.getBytes(StandardCharsets.UTF_8));
    }

    private void signUp(String username) throws Exception {
        mockMvc.perform(post("/api/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"" + username + "\",\"password\":\"" + PASSWORD + "\"}"))
                .andExpect(status().isCreated());
    }

    private String createAccount(String auth, String name) throws Exception {
        return mockMvc.perform(post("/api/accounts")
                        .header("Authorization", auth)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"" + name + "\",\"currency\":\"USD\",\"startingBalance\":1000}"))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
    }

    @BeforeEach
    void setUp() throws Exception {
        signUp("alice");
        signUp("bob");
        String body = createAccount(basic("alice", PASSWORD), "Alice acct");
        aliceAccountId = ((Number) JsonPath.read(body, "$.id")).longValue();
    }

    @Test
    void ownerCanReadOwnAccount() throws Exception {
        mockMvc.perform(get("/api/accounts/" + aliceAccountId).header("Authorization", basic("alice", PASSWORD)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Alice acct"));
    }

    @Test
    void otherUserGets404OnEveryAccountEndpoint() throws Exception {
        String bob = basic("bob", PASSWORD);
        String base = "/api/accounts/" + aliceAccountId;

        mockMvc.perform(get(base).header("Authorization", bob)).andExpect(status().isNotFound());
        mockMvc.perform(get(base + "/portfolio").header("Authorization", bob)).andExpect(status().isNotFound());
        mockMvc.perform(get(base + "/trades").header("Authorization", bob)).andExpect(status().isNotFound());
        mockMvc.perform(post(base + "/orders")
                        .header("Authorization", bob)
                        .header("Idempotency-Key", "bob-key-1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"assetId\":1,\"side\":\"BUY\",\"quantity\":1}"))
                .andExpect(status().isNotFound());
    }

    @Test
    void otherUsersAccountLooksLikeAMissingOne() throws Exception {
        // Same status and body shape as an id that does not exist, so ids cannot be probed.
        mockMvc.perform(get("/api/accounts/" + aliceAccountId).header("Authorization", basic("bob", PASSWORD)))
                .andExpect(status().isNotFound());
        mockMvc.perform(get("/api/accounts/999999").header("Authorization", basic("bob", PASSWORD)))
                .andExpect(status().isNotFound());
    }

    @Test
    void listShowsOnlyOwnAccounts() throws Exception {
        createAccount(basic("bob", PASSWORD), "Bob acct");

        mockMvc.perform(get("/api/accounts").header("Authorization", basic("bob", PASSWORD)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].name").value("Bob acct"));
    }

    @Test
    void adminSeesAndReadsEveryAccount() throws Exception {
        String admin = basic("admin", "test-only-password");

        mockMvc.perform(get("/api/accounts/" + aliceAccountId).header("Authorization", admin))
                .andExpect(status().isOk());
        mockMvc.perform(get("/api/accounts").header("Authorization", admin))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.name=='Alice acct')]").exists());
    }

    @Test
    void anonymousIsStillUnauthorized() throws Exception {
        mockMvc.perform(get("/api/accounts/" + aliceAccountId)).andExpect(status().isUnauthorized());
    }
}