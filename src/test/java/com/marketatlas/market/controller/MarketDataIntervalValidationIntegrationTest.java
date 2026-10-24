package com.marketatlas.market.controller;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;
import com.marketatlas.market.TestPostgresConfiguration;
import org.springframework.context.annotation.Import;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Import(TestPostgresConfiguration.class)
class MarketDataIntervalValidationIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void shouldRejectUnsupportedIntervalThroughApi() throws Exception {
        mockMvc.perform(get("/api/market-data/history")
                .param("symbol", "AAPL")
                .param("startDate", "2026-10-01")
                .param("endDate", "2026-10-05")
                .param("interval", "abc"))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.title").value("Bad Request"))
            .andExpect(jsonPath("$.detail")
                .value("Unsupported interval: abc. Only 1day is currently supported."));
    }
}
