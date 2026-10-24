package com.marketatlas.market.controller;

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

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Import(TestPostgresConfiguration.class)
@Transactional
class AssetApiIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    private int createIssuer(String name) throws Exception {
        String body = mockMvc.perform(post("/api/issuers")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\": \"" + name + "\", \"country\": \"United States\"}"))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return JsonPath.read(body, "$.id");
    }

    private String assetJson(String name, String assetClass, Integer issuerId, String symbol) {
        return "{\"name\": \"" + name + "\", \"assetClass\": \"" + assetClass + "\""
                + (issuerId == null ? "" : ", \"issuerId\": " + issuerId)
                + (symbol == null ? "" : ", \"symbol\": \"" + symbol + "\"")
                + "}";
    }

    @Test
    void shouldCreateEquityAndFindItBySymbol() throws Exception {
        int issuerId = createIssuer("Apple Inc.");

        mockMvc.perform(post("/api/assets")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(assetJson("Apple", "EQUITY", issuerId, " aapl ")))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.symbol").value("AAPL"))
                .andExpect(jsonPath("$.issuerId").value(issuerId));

        mockMvc.perform(get("/api/assets").param("symbol", "aapl"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].name").value("Apple"))
                .andExpect(jsonPath("$[0].symbol").value("AAPL"));
    }

    @Test
    void shouldRejectDuplicateSymbolWith409() throws Exception {
        int issuerId = createIssuer("Apple Inc.");

        mockMvc.perform(post("/api/assets")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(assetJson("Apple", "EQUITY", issuerId, "AAPL")))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/api/assets")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(assetJson("Apple Again", "EQUITY", issuerId, "aapl")))
                .andExpect(status().isConflict());
    }

    @Test
    void shouldCreateCommodityWithoutIssuerOrSymbol() throws Exception {
        String body = mockMvc.perform(post("/api/assets")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(assetJson("Gold", "COMMODITY", null, null)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        int id = JsonPath.read(body, "$.id");

        mockMvc.perform(get("/api/assets/" + id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Gold"))
                .andExpect(jsonPath("$.assetClass").value("COMMODITY"))
                .andExpect(jsonPath("$.issuerId").doesNotExist())
                .andExpect(jsonPath("$.symbol").doesNotExist());
    }

    @Test
    void shouldEnforceIssuerRulesAndLookups() throws Exception {
        mockMvc.perform(post("/api/assets")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(assetJson("Apple", "EQUITY", null, "AAPL")))
                .andExpect(status().isBadRequest());

        mockMvc.perform(post("/api/assets")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(assetJson("Apple", "EQUITY", 999999, "AAPL")))
                .andExpect(status().isNotFound());

        mockMvc.perform(get("/api/assets/999999"))
                .andExpect(status().isNotFound());
    }
}