package com.marketatlas.market.service;

import com.marketatlas.enums.AssetClass;
import com.marketatlas.market.TestPostgresConfiguration;
import com.marketatlas.market.entity.Asset;
import com.marketatlas.market.repository.AssetRepository;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.transaction.annotation.Transactional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

@SpringBootTest
@Import(TestPostgresConfiguration.class)
@Transactional
class AssetCurrencyUnitTest {

    @Autowired
    private AssetService assetService;

    @Autowired
    private AssetRepository assetRepository;

    @Autowired
    private EntityManager entityManager;

    private Asset reload(Asset asset) {
        entityManager.flush();
        entityManager.clear();
        return assetRepository.findById(asset.getId()).orElseThrow();
    }

    @Test
    void shouldStoreNormalizedCurrencyAndUnit() {
        Asset created = assetService.createAsset(
                "Gold Spot", AssetClass.COMMODITY, null, null, " usd ", " troy ounce ");

        Asset stored = reload(created);

        assertEquals("USD", stored.getCurrency());
        assertEquals("troy ounce", stored.getUnit());
    }

    @Test
    void shouldTreatBlankCurrencyAndUnitAsNotProvided() {
        Asset created = assetService.createAsset(
                "Silver Spot", AssetClass.COMMODITY, null, null, "  ", "");

        Asset stored = reload(created);

        assertNull(stored.getCurrency());
        assertNull(stored.getUnit());
    }

    @Test
    void shouldRejectInvalidCurrencyCode() {
        assertThrows(IllegalArgumentException.class, () -> assetService.createAsset(
                "Copper", AssetClass.COMMODITY, null, null, "US", null));
        assertThrows(IllegalArgumentException.class, () -> assetService.createAsset(
                "Copper", AssetClass.COMMODITY, null, null, "US1", null));
    }

    @Test
    void shouldRejectOverlongUnit() {
        assertThrows(IllegalArgumentException.class, () -> assetService.createAsset(
                "Copper", AssetClass.COMMODITY, null, null, "USD", "x".repeat(51)));
    }
}