package com.marketatlas.backtest;

import com.marketatlas.enums.AssetClass;
import com.marketatlas.enums.SeriesProvider;
import com.marketatlas.market.TestPostgresConfiguration;
import com.marketatlas.market.entity.Asset;
import com.marketatlas.market.entity.DataSeries;
import com.marketatlas.market.entity.MarketData;
import com.marketatlas.market.entity.SeriesObservation;
import com.marketatlas.market.repository.AssetRepository;
import com.marketatlas.market.repository.DataSeriesRepository;
import com.marketatlas.market.repository.MarketDataRepository;
import com.marketatlas.market.repository.SeriesObservationRepository;
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

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Base64;

import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Import(TestPostgresConfiguration.class)
@Transactional
class BacktestRealTermsApiIntegrationTest {

    private static final String PASSWORD = "a-long-test-password";
    private static final String JSON = "{\"startDate\":\"2024-01-01\",\"endDate\":\"2024-02-29\","
            + "\"strategy\":\"BUY_AND_HOLD\",\"initialCash\":1000,\"commission\":0}";

    @Autowired MockMvc mockMvc;
    @Autowired AssetRepository assetRepository;
    @Autowired MarketDataRepository marketDataRepository;
    @Autowired DataSeriesRepository dataSeriesRepository;
    @Autowired SeriesObservationRepository observationRepository;

    private String auth;

    private static String basic(String user) {
        String raw = user + ":" + PASSWORD;
        return "Basic " + Base64.getEncoder().encodeToString(raw.getBytes(StandardCharsets.UTF_8));
    }

    @BeforeEach
    void signUp() throws Exception {
        mockMvc.perform(post("/api/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"gina\",\"password\":\"" + PASSWORD + "\"}"))
                .andExpect(status().isCreated());
        auth = basic("gina");
    }

    private Long assetWithBars(String currency) {
        Asset asset = assetRepository.save(new Asset("Real Terms " + System.nanoTime(),
                AssetClass.COMMODITY, null, currency, "troy ounce"));
        bar(asset, 10, 10, LocalDate.of(2024, 1, 2));
        bar(asset, 10, 10, LocalDate.of(2024, 1, 3));
        bar(asset, 11, 11, LocalDate.of(2024, 2, 5));
        bar(asset, 11, 11, LocalDate.of(2024, 2, 6));
        return asset.getId();
    }

    private void bar(Asset asset, double open, double close, LocalDate date) {
        marketDataRepository.save(new MarketData(open, open, open, close, 1.0, asset, date));
    }

    private void seedCpi() {
        DataSeries cpi = dataSeriesRepository.findByProviderAndExternalId(SeriesProvider.FRED, "CPIAUCNS")
                .orElseThrow();
        Instant now = Instant.parse("2026-01-01T00:00:00Z");
        observationRepository.save(new SeriesObservation(cpi, LocalDate.of(2024, 1, 1), new BigDecimal("100"), now));
        observationRepository.save(new SeriesObservation(cpi, LocalDate.of(2024, 2, 1), new BigDecimal("110"), now));
    }

    private ResultActions run(Long assetId) throws Exception {
        return mockMvc.perform(post("/api/assets/" + assetId + "/backtest")
                .header("Authorization", auth)
                .contentType(MediaType.APPLICATION_JSON).content(JSON));
    }

    @Test
    void realTermsShowAFlatRealReturnWhenNominalGainEqualsInflation() throws Exception {
        seedCpi();
        run(assetWithBars("USD"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.metrics.totalReturnPct").value(10.0))
                .andExpect(jsonPath("$.realTerms.baseMonth").value("2024-01-01"))
                .andExpect(jsonPath("$.realTerms.baseCpi").value(100.0))
                .andExpect(jsonPath("$.realTerms.latestCpi").value(110.0))
                .andExpect(jsonPath("$.realTerms.inflationPct").value(10.0))
                .andExpect(jsonPath("$.realTerms.realFinalEquity").value(1000.0))
                .andExpect(jsonPath("$.realTerms.realTotalReturnPct").value(0.0))
                .andExpect(jsonPath("$.realTerms.benchmarkRealTotalReturnPct").value(0.0))
                .andExpect(jsonPath("$.realTerms.cpiEstimated").value(false))
                .andExpect(jsonPath("$.realTermsNote").doesNotExist());
    }

    @Test
    void missingCpiDataGivesNullRealTermsWithANoteAndTheBacktestStillRuns() throws Exception {
        run(assetWithBars("USD"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.metrics.totalReturnPct").value(10.0))
                .andExpect(jsonPath("$.realTerms").doesNotExist())
                .andExpect(jsonPath("$.realTermsNote").value(containsString("CPIAUCNS")));
    }

    @Test
    void nonUsdAssetsGetNoRealTermsBecauseTheDeflatorIsUsCpi() throws Exception {
        seedCpi();
        run(assetWithBars("EUR"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.realTerms").doesNotExist())
                .andExpect(jsonPath("$.realTermsNote").value(containsString("USD")));
    }
}