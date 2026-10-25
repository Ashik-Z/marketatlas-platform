package com.marketatlas.market.service;

import com.marketatlas.enums.AssetClass;
import com.marketatlas.market.TestPostgresConfiguration;
import com.marketatlas.market.dto.MarketDataDTO;
import com.marketatlas.market.entity.Asset;
import com.marketatlas.market.entity.Issuer;
import com.marketatlas.market.repository.AssetRepository;
import com.marketatlas.market.repository.IssuerRepository;
import com.marketatlas.market.repository.MarketDataRepository;
import jakarta.persistence.EntityManagerFactory;
import org.hibernate.SessionFactory;
import org.hibernate.stat.Statistics;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@SpringBootTest(properties = "spring.jpa.properties.hibernate.generate_statistics=true")
@Import(TestPostgresConfiguration.class)
class MarketDataPersistenceStatementCountTest {

    private static final int BARS = 50;

    @Autowired
    private MarketDataPersistenceService persistenceService;

    @Autowired
    private MarketDataRepository marketDataRepository;

    @Autowired
    private AssetRepository assetRepository;

    @Autowired
    private IssuerRepository issuerRepository;

    @Autowired
    private EntityManagerFactory entityManagerFactory;

    private Asset asset;
    private Issuer issuer;

    @BeforeEach
    void setUp() {
        issuer = issuerRepository.save(new Issuer("StatementCount Test Inc.", "Testland"));
        asset = assetRepository.save(new Asset("StatementCount Test", AssetClass.EQUITY, issuer));
    }

    @AfterEach
    void cleanUp() {
        marketDataRepository.deleteAll(marketDataRepository.findByAsset(asset));
        assetRepository.delete(asset);
        issuerRepository.delete(issuer);
    }

    private List<MarketDataDTO> bars() {
        List<MarketDataDTO> bars = new ArrayList<>();
        LocalDate day = LocalDate.of(2026, 1, 1);
        for (int i = 0; i < BARS; i++) {
            bars.add(new MarketDataDTO(100.0, 102.0, 99.0, 101.0, 1000.0, day.plusDays(i)));
        }
        return bars;
    }

    @Test
    void shouldWriteAnyBatchInOneStatement() {
        Statistics statistics = entityManagerFactory.unwrap(SessionFactory.class).getStatistics();
        List<MarketDataDTO> bars = bars();

        statistics.clear();
        persistenceService.persistHistoricalData(asset, bars);
        assertEquals(1L, statistics.getPrepareStatementCount());

        statistics.clear();
        persistenceService.persistHistoricalData(asset, bars);
        assertEquals(1L, statistics.getPrepareStatementCount());
    }
}