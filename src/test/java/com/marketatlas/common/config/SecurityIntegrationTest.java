package com.marketatlas.common.config;

import com.marketatlas.market.TestPostgresConfiguration;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Import(TestPostgresConfiguration.class)
class SecurityIntegrationTest {

    private static final String REFRESH =
            "/api/series/999999/refresh?startDate=2026-10-01&endDate=2026-10-20";

    @Autowired
    private MockMvc mockMvc;

    private static String basic(String user, String password) {
        return "Basic " + Base64.getEncoder()
                .encodeToString((user + ":" + password).getBytes(StandardCharsets.UTF_8));
    }

    @Test
    void readEndpointsArePublic() throws Exception {
        mockMvc.perform(get("/api/series")).andExpect(status().isOk());
    }

    @Test
    void refreshWithoutCredentialsIsRejected() throws Exception {
        mockMvc.perform(post(REFRESH)).andExpect(status().isUnauthorized());
    }

    @Test
    void refreshWithWrongPasswordIsRejected() throws Exception {
        mockMvc.perform(post(REFRESH).header("Authorization", basic("admin", "wrong")))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void refreshWithCorrectCredentialsReachesTheService() throws Exception {
        // 404 (unknown series) proves we passed authentication without calling any provider
        mockMvc.perform(post(REFRESH).header("Authorization", basic("admin", "test-only-password")))
                .andExpect(status().isNotFound());
    }

    @Test
    void assetCreationWithoutCredentialsIsRejected() throws Exception {
        mockMvc.perform(post("/api/assets")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\": \"Gold\", \"assetClass\": \"COMMODITY\"}"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void accountReadsRequireCredentials() throws Exception {
        mockMvc.perform(get("/api/accounts")).andExpect(status().isUnauthorized());
        mockMvc.perform(get("/api/accounts").header("Authorization", basic("admin", "test-only-password")))
                .andExpect(status().isOk());
    }

    @Test
    void marketDataHistoryRequiresCredentialsBecauseItCanCallTheProvider() throws Exception {
        mockMvc.perform(get("/api/market-data/history")
                        .param("symbol", "AAPL").param("startDate", "2026-10-01")
                        .param("endDate", "2026-10-02").param("interval", "1day"))
                .andExpect(status().isUnauthorized());
    }
}