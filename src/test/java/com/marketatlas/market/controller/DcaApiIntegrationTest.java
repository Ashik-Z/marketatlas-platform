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
class DcaApiIntegrationTest {

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

        Asset usd = assetRepository.save(new Asset("Dca Gold " + System.nanoTime(),
                AssetClass.COMMODITY, null, "USD", "troy ounce"));
        usdAssetId = usd.getId();
        marketDataRepository.save(new MarketData(50.0, 50.0, 50.0, 50.0, 1.0, usd, LocalDate.of(2001, 1, 15)));
        marketDataRepository.save(new MarketData(55.0, 55.0, 55.0, 55.0, 1.0, usd, LocalDate.of(2001, 2, 12)));
        marketDataRepository.save(new MarketData(60.5, 60.5, 60.5, 60.5, 1.0, usd, LocalDate.of(2001, 3, 10)));
    }

    private String url(Long id, String query) {
        return "/api/assets/" + id + "/dca?" + query;
    }

    @Test
    void pricesThatKeepUpWithInflationGiveANominalButNoRealGain() throws Exception {
        mockMvc.perform(get(url(usdAssetId, "startDate=2001-01-01&endDate=2001-12-31&amount=100&baseMonth=2001-03")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.cpiSeries").value("CPIAUCNS"))
                .andExpect(jsonPath("$.attribution").isNotEmpty())
                .andExpect(jsonPath("$.baseMonth").value("2001-03-01"))
                .andExpect(jsonPath("$.purchaseCount").value(3))
                .andExpect(jsonPath("$.purchases.length()").value(3))
                .andExpect(jsonPath("$.purchases[0].units").value(2.0))
                .andExpect(jsonPath("$.purchases[0].realAmount").value(121.0))
                .andExpect(jsonPath("$.investedNominal").value(300.0))
                .andExpect(jsonPath("$.investedReal").value(331.0))
                .andExpect(jsonPath("$.valueNominal").value(331.0))
                .andExpect(jsonPath("$.valueReal").value(331.0))
                .andExpect(jsonPath("$.nominalGain").value(31.0))
                .andExpect(jsonPath("$.realGain").value(0.0))
                .andExpect(jsonPath("$.realGainPct").value(0.0));
    }

    @Test
    void baseMonthDefaultsToTheLatestCpiMonth() throws Exception {
        mockMvc.perform(get(url(usdAssetId, "startDate=2001-01-01&endDate=2001-12-31&amount=100")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.baseMonth").isNotEmpty());
    }

    @Test
    void badAmountsAreRejected() throws Exception {
        for (String amount : new String[] {"0", "-5", "10.123", "1000000001"}) {
            mockMvc.perform(get(url(usdAssetId, "startDate=2001-01-01&endDate=2001-12-31&amount=" + amount)))
                    .andExpect(status().isBadRequest());
        }
    }

    @Test
    void reversedDatesAndEmptyRangesAreRejected() throws Exception {
        mockMvc.perform(get(url(usdAssetId, "startDate=2001-12-31&endDate=2001-01-01&amount=100")))
                .andExpect(status().isBadRequest());
        mockMvc.perform(get(url(usdAssetId, "startDate=2001-06-01&endDate=2001-06-30&amount=100")))
                .andExpect(status().isBadRequest());
    }

    @Test
    void nonUsdAssetIsRejectedAndUnknownAssetIsNotFound() throws Exception {
        Asset eur = assetRepository.save(new Asset("Dca Euro " + System.nanoTime(),
                AssetClass.COMMODITY, null, "EUR", "unit"));
        mockMvc.perform(get(url(eur.getId(), "startDate=2001-01-01&endDate=2001-12-31&amount=100")))
                .andExpect(status().isBadRequest());
        mockMvc.perform(get(url(999_999_999L, "startDate=2001-01-01&endDate=2001-12-31&amount=100")))
                .andExpect(status().isNotFound());
    }
}
