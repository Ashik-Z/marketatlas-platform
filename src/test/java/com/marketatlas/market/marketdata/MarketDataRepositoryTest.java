package com.marketatlas.market.marketdata;

import com.marketatlas.market.entity.Asset;
import com.marketatlas.market.entity.Issuer;
import com.marketatlas.market.entity.MarketData;
import com.marketatlas.enums.AssetClass;
import com.marketatlas.market.repository.AssetRepository;
import com.marketatlas.market.repository.MarketDataRepository;

import com.marketatlas.market.TestPostgresConfiguration;
import org.springframework.context.annotation.Import;

import com.marketatlas.market.repository.IssuerRepository;

import org.springframework.dao.DataIntegrityViolationException;

import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
// import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import(TestPostgresConfiguration.class)
class MarketDataRepositoryTest {

    @Autowired
    private MarketDataRepository marketDataRepository;

    @Autowired
    private AssetRepository assetRepository;

    @Autowired
    private IssuerRepository issuerRepository;

    @Test
    void shouldSaveMarketData() {

        Issuer apple = new Issuer(
            "Apple Inc.",
            "United States"
        );

        Asset asset = new Asset(
            "Apple",
            "AAPL",
            AssetClass.EQUITY,
            apple
        );

        assetRepository.save(asset);

        LocalDate timestamp = LocalDate.of(2026, 10, 1);

        MarketData marketData = new MarketData(
            254.80,
            255.30,
            254.60,
            255.10,
            183421.0,
            asset,
            timestamp
        );

        MarketData savedMarketData =
            marketDataRepository.save(marketData);

        assertNotNull(savedMarketData.getId());
        assertEquals(255.10, savedMarketData.getClose());
        assertEquals(asset, savedMarketData.getAsset());
        assertEquals(timestamp, savedMarketData.getTimestamp());
    }

    @Test
    void shouldFindMarketDataById() {

        Issuer apple = new Issuer(
            "Apple Inc.",
            "United States"
        );

        Asset asset = new Asset(
            "Apple",
            "AAPL",
            AssetClass.EQUITY,
            apple
        );

        assetRepository.save(asset);

        MarketData marketData = new MarketData(
            254.80,
            255.30,
            254.60,
            255.10,
            183421.0,
            asset,
            LocalDate.of(2026, 10, 1)
        );

        MarketData savedMarketData =
            marketDataRepository.save(marketData);

        var result =
            marketDataRepository.findById(savedMarketData.getId());

        assertTrue(result.isPresent());
        assertEquals(savedMarketData.getId(), result.get().getId());
        assertEquals(asset, result.get().getAsset());
    }

    @Test
    void shouldRejectDuplicateAssetAndTimestamp() {
        Issuer apple = new Issuer("Apple Inc.", "United States");
        issuerRepository.save(apple);

        Asset asset = new Asset("Apple", "AAPL", AssetClass.EQUITY, apple);
        assetRepository.save(asset);

        LocalDate timestamp = LocalDate.of(2026, 10, 1);

        MarketData first = new MarketData(
                254.80, 255.30, 254.60, 255.10, 183421.0, asset, timestamp
        );

        MarketData duplicate = new MarketData(
                255.10, 256.00, 254.90, 255.80, 190000.0, asset, timestamp
        );

        marketDataRepository.saveAndFlush(first);

        assertThrows(
                DataIntegrityViolationException.class,
                () -> marketDataRepository.saveAndFlush(duplicate)
        );
    }
}