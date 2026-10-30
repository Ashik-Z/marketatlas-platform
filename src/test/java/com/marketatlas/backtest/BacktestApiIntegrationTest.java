package com.marketatlas.backtest;

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
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.Base64;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Import(TestPostgresConfiguration.class)
@Transactional
class BacktestApiIntegrationTest {

    private static final String PASSWORD = "a-long-test-password";

    @Autowired MockMvc mockMvc;
    @Autowired AssetRepository assetRepository;
    @Autowired MarketDataRepository marketDataRepository;

    private Long assetId;
    private Long emptyAssetId;
    private String auth;

    private static String basic(String user, String password) {
        String raw = user + ":" + password;
        return "Basic " + Base64.getEncoder().encodeToString(raw.getBytes(StandardCharsets.UTF_8));
    }

    private void bar(Asset asset, double open, double close, LocalDate date) {
        marketDataRepository.save(new MarketData(open, open, open, close, 1.0, asset, date));
    }

    @BeforeEach
    void seed() throws Exception {
        mockMvc.perform(post("/api/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"carol\",\"password\":\"" + PASSWORD + "\"}"))
                .andExpect(status().isCreated());
        auth = basic("carol", PASSWORD);

        Asset asset = assetRepository.save(new Asset("Backtest Gold " + System.nanoTime(),
                AssetClass.COMMODITY, null, "USD", "troy ounce"));
        assetId = asset.getId();
        bar(asset, 10, 10, LocalDate.of(2024, 1, 2));
        bar(asset, 10, 11, LocalDate.of(2024, 1, 3));
        bar(asset, 12, 12, LocalDate.of(2024, 1, 4));
        bar(asset, 12, 15, LocalDate.of(2024, 1, 5));
        bar(asset, 99, 99, LocalDate.of(2024, 1, 6)); // Saturday: must be skipped

        emptyAssetId = assetRepository.save(new Asset("Backtest Empty " + System.nanoTime(),
                AssetClass.COMMODITY, null, "USD", "troy ounce")).getId();
    }

    private static String body(String strategy, String extra) {
        return "{\"startDate\":\"2024-01-01\",\"endDate\":\"2024-01-31\",\"strategy\":\"" + strategy
                + "\",\"initialCash\":1000,\"commission\":0" + extra + "}";
    }

    private ResultActions run(Long id, String authHeader, String json) throws Exception {
        var request = post("/api/assets/" + id + "/backtest")
                .contentType(MediaType.APPLICATION_JSON).content(json);
        if (authHeader != null) {
            request = request.header("Authorization", authHeader);
        }
        return mockMvc.perform(request);
    }

    @Test
    void anonymousCallerGets401() throws Exception {
        run(assetId, null, body("BUY_AND_HOLD", "")).andExpect(status().isUnauthorized());
    }

    @Test
    void buyAndHoldMatchesHandComputedNumbersAndSkipsWeekendBars() throws Exception {
        run(assetId, auth, body("BUY_AND_HOLD", ""))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.strategy").value("BUY_AND_HOLD"))
                .andExpect(jsonPath("$.barsUsed").value(4))
                .andExpect(jsonPath("$.skippedBars").value(1))
                .andExpect(jsonPath("$.firstBarDate").value("2024-01-02"))
                .andExpect(jsonPath("$.lastBarDate").value("2024-01-05"))
                .andExpect(jsonPath("$.metrics.totalReturnPct").value(50.0))
                .andExpect(jsonPath("$.benchmark.totalReturnPct").value(50.0))
                .andExpect(jsonPath("$.trades.length()").value(1))
                .andExpect(jsonPath("$.trades[0].side").value("BUY"))
                .andExpect(jsonPath("$.trades[0].date").value("2024-01-03"))
                .andExpect(jsonPath("$.trades[0].price").value(10.0))
                .andExpect(jsonPath("$.trades[0].quantity").value(100.0))
                .andExpect(jsonPath("$.equityCurve").doesNotExist());
    }

    @Test
    void equityCurvesAreIncludedOnlyWhenRequested() throws Exception {
        run(assetId, auth, body("BUY_AND_HOLD", ",\"includeEquityCurve\":true"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.equityCurve.length()").value(4))
                .andExpect(jsonPath("$.equityCurve[3].equity").value(1500.0))
                .andExpect(jsonPath("$.benchmarkEquityCurve.length()").value(4));
    }

    @Test
    void smaCrossoverRunsAgainstTheBenchmark() throws Exception {
        run(assetId, auth, body("SMA_CROSSOVER", ",\"shortWindow\":1,\"longWindow\":2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.strategy").value("SMA_CROSSOVER_1_2"))
                .andExpect(jsonPath("$.trades.length()").value(1))
                .andExpect(jsonPath("$.trades[0].side").value("BUY"))
                .andExpect(jsonPath("$.trades[0].date").value("2024-01-04"))
                .andExpect(jsonPath("$.benchmark.totalReturnPct").value(50.0));
    }

    @Test
    void smaCrossoverWithoutWindowsIs400() throws Exception {
        run(assetId, auth, body("SMA_CROSSOVER", "")).andExpect(status().isBadRequest());
    }

    @Test
    void shortWindowNotBelowLongWindowIs400() throws Exception {
        run(assetId, auth, body("SMA_CROSSOVER", ",\"shortWindow\":5,\"longWindow\":3"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void startAfterEndIs400() throws Exception {
        run(assetId, auth, "{\"startDate\":\"2024-02-01\",\"endDate\":\"2024-01-01\","
                + "\"strategy\":\"BUY_AND_HOLD\",\"initialCash\":1000}")
                .andExpect(status().isBadRequest());
    }

    @Test
    void invalidInputsAreRejected() throws Exception {
        run(assetId, auth, body("BUY_AND_HOLD", ",\"slippageBps\":5000")).andExpect(status().isBadRequest());
        run(assetId, auth, "{\"startDate\":\"2024-01-01\",\"endDate\":\"2024-01-31\","
                + "\"strategy\":\"BUY_AND_HOLD\",\"initialCash\":-5}").andExpect(status().isBadRequest());
        run(assetId, auth, "{\"startDate\":\"2024-01-01\",\"endDate\":\"2024-01-31\","
                + "\"strategy\":\"NOPE\",\"initialCash\":1000}").andExpect(status().isBadRequest());
    }

    @Test
    void unknownAssetIs404() throws Exception {
        run(999_999_999L, auth, body("BUY_AND_HOLD", "")).andExpect(status().isNotFound());
    }

    @Test
    void rangeWithFewerThanTwoBarsIs400() throws Exception {
        run(emptyAssetId, auth, body("BUY_AND_HOLD", "")).andExpect(status().isBadRequest());
    }
}