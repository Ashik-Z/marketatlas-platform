package com.marketatlas.watchlist.controller;

import com.marketatlas.enums.AssetClass;
import com.marketatlas.market.TestPostgresConfiguration;
import com.marketatlas.market.entity.Asset;
import com.marketatlas.market.entity.MarketData;
import com.marketatlas.market.repository.AssetRepository;
import com.marketatlas.market.repository.MarketDataRepository;
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
import java.time.LocalDate;
import java.util.Base64;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Import(TestPostgresConfiguration.class)
@Transactional
class WatchlistIntegrationTest {

    private static final String PASSWORD = "a-long-test-password";

    @Autowired MockMvc mockMvc;
    @Autowired AssetRepository assetRepository;
    @Autowired MarketDataRepository marketDataRepository;

    private String alice;
    private String bob;
    private Asset gold;

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

    private Asset newAsset(String name) {
        return assetRepository.save(new Asset(name + " " + System.nanoTime(),
                AssetClass.COMMODITY, null, "USD", "troy ounce"));
    }

    @BeforeEach
    void setUp() throws Exception {
        signUp("alice");
        signUp("bob");
        alice = basic("alice");
        bob = basic("bob");
        gold = newAsset("Watch Gold");
        marketDataRepository.save(new MarketData(100.0, 100.0, 100.0, 100.0, 1.0, gold, LocalDate.of(2026, 10, 1)));
        marketDataRepository.save(new MarketData(110.0, 110.0, 110.0, 110.0, 1.0, gold, LocalDate.of(2026, 10, 2)));
    }

    @Test
    void anonymousRequestsAreRejected() throws Exception {
        mockMvc.perform(get("/api/watchlist")).andExpect(status().isUnauthorized());
        mockMvc.perform(put("/api/watchlist/" + gold.getId())).andExpect(status().isUnauthorized());
        mockMvc.perform(delete("/api/watchlist/" + gold.getId())).andExpect(status().isUnauthorized());
    }

    @Test
    void addIsIdempotentAndListShowsNominalChange() throws Exception {
        mockMvc.perform(put("/api/watchlist/" + gold.getId()).header("Authorization", alice))
                .andExpect(status().isNoContent());
        mockMvc.perform(put("/api/watchlist/" + gold.getId()).header("Authorization", alice))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/watchlist").header("Authorization", alice))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].assetId").value(gold.getId()))
                .andExpect(jsonPath("$[0].latestClose").value(110.0))
                .andExpect(jsonPath("$[0].previousClose").value(100.0))
                .andExpect(jsonPath("$[0].change").value(10.0))
                .andExpect(jsonPath("$[0].changePercent").value(10.0))
                .andExpect(jsonPath("$[0].latestDate").value("2026-10-02"));
    }

    @Test
    void unknownAssetIs404() throws Exception {
        mockMvc.perform(put("/api/watchlist/999999999").header("Authorization", alice))
                .andExpect(status().isNotFound());
    }

    @Test
    void assetWithoutBarsHasNullPrices() throws Exception {
        Asset empty = newAsset("Watch Empty");
        mockMvc.perform(put("/api/watchlist/" + empty.getId()).header("Authorization", alice))
                .andExpect(status().isNoContent());
        mockMvc.perform(get("/api/watchlist").header("Authorization", alice))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].latestClose").doesNotExist())
                .andExpect(jsonPath("$[0].change").doesNotExist());
    }

    @Test
    void usersOnlySeeAndChangeTheirOwnWatchlist() throws Exception {
        mockMvc.perform(put("/api/watchlist/" + gold.getId()).header("Authorization", alice))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/watchlist").header("Authorization", bob))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));

        // Bob removing the same asset must not touch Alice's entry.
        mockMvc.perform(delete("/api/watchlist/" + gold.getId()).header("Authorization", bob))
                .andExpect(status().isNoContent());
        mockMvc.perform(get("/api/watchlist").header("Authorization", alice))
                .andExpect(jsonPath("$.length()").value(1));
    }

    @Test
    void removeIsIdempotent() throws Exception {
        mockMvc.perform(put("/api/watchlist/" + gold.getId()).header("Authorization", alice))
                .andExpect(status().isNoContent());
        mockMvc.perform(delete("/api/watchlist/" + gold.getId()).header("Authorization", alice))
                .andExpect(status().isNoContent());
        mockMvc.perform(delete("/api/watchlist/" + gold.getId()).header("Authorization", alice))
                .andExpect(status().isNoContent());
        mockMvc.perform(get("/api/watchlist").header("Authorization", alice))
                .andExpect(jsonPath("$.length()").value(0));
    }

    @Test
    void fiftyFirstDistinctAssetIsRejectedButExistingOnesStillAccepted() throws Exception {
        mockMvc.perform(put("/api/watchlist/" + gold.getId()).header("Authorization", alice))
                .andExpect(status().isNoContent());
        for (int i = 1; i < 50; i++) {
            Asset a = newAsset("Cap " + i);
            mockMvc.perform(put("/api/watchlist/" + a.getId()).header("Authorization", alice))
                    .andExpect(status().isNoContent());
        }
        Asset extra = newAsset("Cap extra");
        mockMvc.perform(put("/api/watchlist/" + extra.getId()).header("Authorization", alice))
                .andExpect(status().isConflict());
        // Re-adding something already watched is a no-op, not a limit error.
        mockMvc.perform(put("/api/watchlist/" + gold.getId()).header("Authorization", alice))
                .andExpect(status().isNoContent());
    }
}
