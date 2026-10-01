package com.marketatlas.market.marketdata;

import com.marketatlas.market.entity.Asset;
import com.marketatlas.market.entity.Issuer;
import com.marketatlas.market.entity.MarketData;
import com.marketatlas.enums.AssetClass;
import com.marketatlas.market.repository.AssetRepository;
import com.marketatlas.market.repository.MarketDataRepository;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
// import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;

import java.time.Instant;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class MarketDataRepositoryTest {

    @Autowired
    private MarketDataRepository marketDataRepository;

    @Autowired
    private AssetRepository assetRepository;

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

        Instant timestamp = Instant.parse(
            "2026-10-01T10:00:00Z"
        );

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
            Instant.parse("2026-10-01T10:00:00Z")
        );

        MarketData savedMarketData =
            marketDataRepository.save(marketData);

        var result =
            marketDataRepository.findById(savedMarketData.getId());

        assertTrue(result.isPresent());
        assertEquals(savedMarketData.getId(), result.get().getId());
        assertEquals(asset, result.get().getAsset());
    }
}