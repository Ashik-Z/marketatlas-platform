package com.marketatlas.market.service;

import com.marketatlas.enums.AssetClass;
import com.marketatlas.market.TestPostgresConfiguration;
import com.marketatlas.market.dto.MarketDataDTO;
import com.marketatlas.market.entity.Asset;
import com.marketatlas.market.entity.Issuer;
import com.marketatlas.market.entity.MarketData;
import com.marketatlas.market.repository.AssetRepository;
import com.marketatlas.market.repository.IssuerRepository;
import com.marketatlas.market.repository.MarketDataRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@Import(TestPostgresConfiguration.class)
@Transactional
class MarketDataPersistenceServiceTest {

    @Autowired
    private MarketDataPersistenceService marketDataPersistenceService;

    @Autowired
    private MarketDataRepository marketDataRepository;

    @Autowired
    private AssetRepository assetRepository;

    @Autowired
    private IssuerRepository issuerRepository;

    private Asset apple;

    @BeforeEach
    void setUp() {
        Issuer issuer = issuerRepository.save(
                new Issuer("Apple Inc.", "United States")
        );

        apple = assetRepository.save(
                new Asset("Apple", AssetClass.EQUITY, issuer)
        );
    }

    @Test
    void shouldInsertNewMarketData() {
        LocalDate timestamp = LocalDate.of(2026, 10, 1);

        MarketDataDTO dto = new MarketDataDTO(
                254.80, 255.30, 254.60, 255.10, 183421.0, timestamp
        );

        List<MarketData> result =
                marketDataPersistenceService.persistHistoricalData(
                        apple, List.of(dto)
                );

        assertEquals(1, result.size());

        MarketData saved = result.get(0);

        assertNotNull(saved.getId());
        assertEquals(254.80, saved.getOpen());
        assertEquals(255.30, saved.getHigh());
        assertEquals(254.60, saved.getLow());
        assertEquals(255.10, saved.getClose());
        assertEquals(183421.0, saved.getVolume());
        assertEquals(apple, saved.getAsset());
        assertEquals(timestamp, saved.getTimestamp());
        assertEquals(1, marketDataRepository.count());
    }

    @Test
    void shouldUpdateExistingMarketData() {
        LocalDate timestamp = LocalDate.of(2026, 10, 1);

        MarketData existing = marketDataRepository.saveAndFlush(
                new MarketData(
                        250.0, 255.0, 249.0, 252.0, 100000.0,
                        apple, timestamp
                )
        );

        Long existingId = existing.getId();

        MarketDataDTO updatedDTO = new MarketDataDTO(
                254.80, 256.0, 254.60, 255.10, 183421.0, timestamp
        );

        List<MarketData> result =
                marketDataPersistenceService.persistHistoricalData(
                        apple, List.of(updatedDTO)
                );

        assertEquals(1, result.size());
        assertEquals(existingId, result.get(0).getId());
        assertEquals(254.80, result.get(0).getOpen());
        assertEquals(256.0, result.get(0).getHigh());
        assertEquals(254.60, result.get(0).getLow());
        assertEquals(255.10, result.get(0).getClose());
        assertEquals(183421.0, result.get(0).getVolume());
        assertEquals(1, marketDataRepository.count());
    }

    @Test
    void shouldPersistMixedBatchOfExistingAndNewMarketData() {
        LocalDate existingDate = LocalDate.of(2026, 10, 1);
        LocalDate newDate = LocalDate.of(2026, 10, 2);

        MarketData existing = marketDataRepository.saveAndFlush(
                new MarketData(
                        250.0, 255.0, 249.0, 252.0, 100000.0,
                        apple, existingDate
                )
        );

        Long existingId = existing.getId();

        List<MarketDataDTO> dtos = List.of(
                new MarketDataDTO(
                        254.80, 256.0, 254.60, 255.10,
                        183421.0, existingDate
                ),
                new MarketDataDTO(
                        255.10, 257.0, 254.90, 256.50,
                        200000.0, newDate
                )
        );

        List<MarketData> result =
                marketDataPersistenceService.persistHistoricalData(
                        apple, dtos
                );

        assertEquals(2, result.size());
        assertEquals(existingId, result.get(0).getId());
        assertEquals(255.10, result.get(0).getClose());
        assertNotNull(result.get(1).getId());
        assertEquals(newDate, result.get(1).getTimestamp());
        assertEquals(256.50, result.get(1).getClose());
        assertEquals(2, marketDataRepository.count());
    }

    @Test
    void shouldReturnEmptyListWhenInputIsEmpty() {
        List<MarketData> result =
                marketDataPersistenceService.persistHistoricalData(
                        apple, List.of()
                );

        assertNotNull(result);
        assertTrue(result.isEmpty());
        assertEquals(0, marketDataRepository.count());
    }
}
