package com.marketatlas.market.controller;

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
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Import(TestPostgresConfiguration.class)
@Transactional
class RealPriceApiIntegrationTest {

    @Autowired MockMvc mockMvc;
    @Autowired AssetRepository assetRepository;
    @Autowired MarketDataRepository marketDataRepository;
    @Autowired DataSeriesRepository dataSeriesRepository;
    @Autowired SeriesObservationRepository observationRepository;

    private Long usdAssetId;

    @BeforeEach
    void seed() {
        DataSeries cpi = dataSeriesRepository.findByProviderAndExternalId(SeriesProvider.FRED, "CPIAUCNS")
                .orElseThrow();
        Instant now = Instant.parse("2026-01-01T00:00:00Z");
        observationRepository.save(new SeriesObservation(cpi, LocalDate.of(2001, 1, 1), new BigDecimal("100"), now));
        observationRepository.save(new SeriesObservation(cpi, LocalDate.of(2001, 2, 1), new BigDecimal("110"), now));
        observationRepository.save(new SeriesObservation(cpi, LocalDate.of(2001, 3, 1), new BigDecimal("121"), now));

        Asset usd = assetRepository.save(new Asset("Real Gold " + System.nanoTime(),
                AssetClass.COMMODITY, null, "USD", "troy ounce"));
        usdAssetId = usd.getId();
        marketDataRepository.save(new MarketData(50.0, 50.0, 50.0, 50.0, 1.0, usd, LocalDate.of(2001, 1, 15)));
        marketDataRepository.save(new MarketData(60.5, 60.5, 60.5, 60.5, 1.0, usd, LocalDate.of(2001, 3, 10)));
        marketDataRepository.save(new MarketData(60.5, 60.5, 60.5, 60.5, 1.0, usd, LocalDate.of(2001, 5, 2)));
    }

    private String url(Long id, String query) {
        return "/api/assets/" + id + "/real-prices?" + query;
    }

    @Test
    void realPricesAreExpressedInBaseMonthDollarsWithoutLogin() throws Exception {
        mockMvc.perform(get(url(usdAssetId, "startDate=2001-01-01&endDate=2001-12-31&baseMonth=2001-03")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.cpiSeries").value("CPIAUCNS"))
                .andExpect(jsonPath("$.attribution").isNotEmpty())
                .andExpect(jsonPath("$.baseMonth").value("2001-03-01"))
                .andExpect(jsonPath("$.points.length()").value(3))
                .andExpect(jsonPath("$.points[0].nominalClose").value(50.0))
                .andExpect(jsonPath("$.points[0].realClose").value(60.5))
                .andExpect(jsonPath("$.points[0].cpiMonth").value("2001-01-01"))
                .andExpect(jsonPath("$.points[0].cpiEstimated").value(false))
                .andExpect(jsonPath("$.nominalReturnPct").value(21.0))
                .andExpect(jsonPath("$.realReturnPct").value(0.0));
    }

    @Test
    void monthWithoutPublishedCpiIsFlaggedAsEstimated() throws Exception {
        mockMvc.perform(get(url(usdAssetId, "startDate=2001-01-01&endDate=2001-12-31&baseMonth=2001-03")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.points[2].cpiMonth").value("2001-03-01"))
                .andExpect(jsonPath("$.points[2].cpiEstimated").value(true));
    }

    @Test
    void baseMonthDefaultsToTheLatestCpiMonth() throws Exception {
        mockMvc.perform(get(url(usdAssetId, "startDate=2001-01-01&endDate=2001-12-31")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.baseMonth").isNotEmpty());
    }

    @Test
    void emptyRangeReturnsNoPointsAndNoReturns() throws Exception {
        mockMvc.perform(get(url(usdAssetId, "startDate=2001-06-01&endDate=2001-06-30&baseMonth=2001-03")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.points.length()").value(0))
                .andExpect(jsonPath("$.nominalReturnPct").doesNotExist())
                .andExpect(jsonPath("$.realReturnPct").doesNotExist());
    }

    @Test
    void nonUsdAssetIsRejected() throws Exception {
        Asset eur = assetRepository.save(new Asset("Euro thing " + System.nanoTime(),
                AssetClass.COMMODITY, null, "EUR", "unit"));

        mockMvc.perform(get(url(eur.getId(), "startDate=2001-01-01&endDate=2001-12-31")))
                .andExpect(status().isBadRequest());
    }

    @Test
    void assetWithoutCurrencyIsRejected() throws Exception {
        Asset none = assetRepository.save(new Asset("No currency " + System.nanoTime(),
                AssetClass.COMMODITY, null, null, null));

        mockMvc.perform(get(url(none.getId(), "startDate=2001-01-01&endDate=2001-12-31")))
                .andExpect(status().isBadRequest());
    }

    @Test
    void unknownAssetIs404() throws Exception {
        mockMvc.perform(get(url(999999L, "startDate=2001-01-01&endDate=2001-12-31")))
                .andExpect(status().isNotFound());
    }

    @Test
    void baseMonthWithoutCpiIs400() throws Exception {
        mockMvc.perform(get(url(usdAssetId, "startDate=2001-01-01&endDate=2001-12-31&baseMonth=1990-01")))
                .andExpect(status().isBadRequest());
    }

    @Test
    void invalidRangesAre400() throws Exception {
        mockMvc.perform(get(url(usdAssetId, "startDate=2001-12-31&endDate=2001-01-01")))
                .andExpect(status().isBadRequest());
        mockMvc.perform(get(url(usdAssetId, "startDate=1990-01-01&endDate=2026-01-01")))
                .andExpect(status().isBadRequest());
    }
}