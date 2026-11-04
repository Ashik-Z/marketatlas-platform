package com.marketatlas.common.config;

import com.marketatlas.market.TestPostgresConfiguration;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Import(TestPostgresConfiguration.class)
class DefaultDenyTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void unlistedGetPathsRequireLogin() throws Exception {
        mockMvc.perform(get("/api/not-a-real-endpoint")).andExpect(status().isUnauthorized());
        mockMvc.perform(get("/api/assets/1/not-a-real-endpoint")).andExpect(status().isUnauthorized());
        mockMvc.perform(get("/api/series/1")).andExpect(status().isUnauthorized());
    }

    @Test
    void listedPublicReadsStayOpen() throws Exception {
        for (String path : new String[] {
                "/api/assets", "/api/assets/999999", "/api/assets/999999/real-prices",
                "/api/issuers", "/api/issuers/999999",
                "/api/series", "/api/series/999999/observations"}) {
            mockMvc.perform(get(path))
                    .andExpect(status().is(not(401)))
                    .andExpect(status().is(not(403)));
        }
    }
}
