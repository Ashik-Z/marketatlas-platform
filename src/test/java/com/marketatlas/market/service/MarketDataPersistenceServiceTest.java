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
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

@SpringBootTest
@Import(TestPostgresConfiguration.class)
@Transactional
class MarketDataPersistenceServiceTest {

    private static final LocalDate D1 = LocalDate.of(2026, 1, 5);
    private static final LocalDate D2 = LocalDate.of(2026, 1, 6);
    private static final LocalDate D3 = LocalDate.of(2026, 1, 7);

    @Autowired
    private MarketDataPersistenceService persistenceService;

    @Autowired
    private MarketDataRepository marketDataRepository;

    @Autowired
    private AssetRepository assetRepository;

    @Autowired
    private IssuerRepository issuerRepository;

    @Autowired
    private EntityManager entityManager;

    private Asset asset;

    @BeforeEach
    void setUp() {
        Issuer issuer = issuerRepository.save(new Issuer("Persistence Test Inc.", "Testland"));
        asset = assetRepository.save(new Asset("Persistence Test", AssetClass.EQUITY, issuer));
        entityManager.flush();
    }

    private MarketDataDTO bar(LocalDate date, double close) {
        return new MarketDataDTO(close - 1, close + 1, close - 2, close, 1000.0, date);
    }

    private List<MarketData> rows(LocalDate from, LocalDate to) {
        entityManager.clear();
        return marketDataRepository.findByAssetAndTimestampBetweenOrderByTimestampAsc(asset, from, to);
    }

    @Test
    void shouldInsertNewBar() {
        int written = persistenceService.persistHistoricalData(asset, List.of(bar(D1, 100.0)));

        assertEquals(1, written);
        List<MarketData> rows = rows(D1, D1);
        assertEquals(1, rows.size());
        assertEquals(D1, rows.get(0).getTimestamp());
        assertEquals(99.0, rows.get(0).getOpen().doubleValue());
        assertEquals(101.0, rows.get(0).getHigh().doubleValue());
        assertEquals(98.0, rows.get(0).getLow().doubleValue());
        assertEquals(100.0, rows.get(0).getClose().doubleValue());
        assertEquals(1000.0, rows.get(0).getVolume().doubleValue());
        assertEquals(asset.getId(), rows.get(0).getAsset().getId());
    }

    @Test
    void shouldUpdateExistingBarKeepingItsId() {
        persistenceService.persistHistoricalData(asset, List.of(bar(D1, 100.0)));
        Long originalId = rows(D1, D1).get(0).getId();

        int written = persistenceService.persistHistoricalData(asset, List.of(bar(D1, 150.0)));

        assertEquals(1, written);
        List<MarketData> rows = rows(D1, D1);
        assertEquals(1, rows.size());
        assertEquals(originalId, rows.get(0).getId());
        assertEquals(150.0, rows.get(0).getClose().doubleValue());
    }

    @Test
    void shouldHandleMixedBatchOfExistingAndNewBars() {
        persistenceService.persistHistoricalData(asset, List.of(bar(D1, 100.0)));

        int written = persistenceService.persistHistoricalData(asset,
                List.of(bar(D1, 110.0), bar(D2, 120.0)));

        assertEquals(2, written);
        List<MarketData> rows = rows(D1, D2);
        assertEquals(2, rows.size());
        assertEquals(110.0, rows.get(0).getClose().doubleValue());
        assertEquals(120.0, rows.get(1).getClose().doubleValue());
    }

    @Test
    void shouldAcceptBarsInAnyOrder() {
        int written = persistenceService.persistHistoricalData(asset,
                List.of(bar(D3, 30.0), bar(D1, 10.0), bar(D2, 20.0)));

        assertEquals(3, written);
        List<MarketData> rows = rows(D1, D3);
        assertEquals(List.of(D1, D2, D3), rows.stream().map(MarketData::getTimestamp).toList());
    }

    @Test
    void shouldKeepLastBarWhenDatesAreDuplicated() {
        int written = persistenceService.persistHistoricalData(asset,
                List.of(bar(D1, 10.0), bar(D1, 99.0)));

        assertEquals(1, written);
        List<MarketData> rows = rows(D1, D1);
        assertEquals(1, rows.size());
        assertEquals(99.0, rows.get(0).getClose().doubleValue());
    }

    @Test
    void shouldDoNothingForEmptyInput() {
        int written = persistenceService.persistHistoricalData(asset, List.of());

        assertEquals(0, written);
        assertEquals(0, rows(D1, D3).size());
    }

    @Test
    void shouldPersistBarWithoutVolume() {
        MarketDataDTO noVolume = new MarketDataDTO(10.0, 11.0, 9.0, 10.5, null, D1);

        int written = persistenceService.persistHistoricalData(asset, List.of(noVolume, bar(D2, 20.0)));

        assertEquals(2, written);
        List<MarketData> rows = rows(D1, D2);
        assertEquals(2, rows.size());
        assertEquals(null, rows.get(0).getVolume());
        assertEquals(1000.0, rows.get(1).getVolume().doubleValue());
    }
}