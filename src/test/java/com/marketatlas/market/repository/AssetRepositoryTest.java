package com.marketatlas.market.repository;

import com.marketatlas.enums.AssetClass;
import com.marketatlas.market.entity.Asset;
import com.marketatlas.market.entity.Issuer;
import com.marketatlas.market.TestPostgresConfiguration;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.context.annotation.Import;

import com.marketatlas.enums.IdentifierScheme;
import com.marketatlas.market.entity.AssetIdentifier;
import org.springframework.dao.DataIntegrityViolationException;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;

import java.util.Optional;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import(TestPostgresConfiguration.class)
class AssetRepositoryTest {

    @Autowired
    private AssetRepository assetRepository;

    @Autowired
    private AssetIdentifierRepository assetIdentifierRepository;

    @Autowired
    private IssuerRepository issuerRepository;

    @Test
    void shouldSaveAsset() {

        // Create an issuer
        Issuer apple = new Issuer(
            "Apple Inc.",
            "United States"
        );

        // Persist the issuer first
        issuerRepository.save(apple);

        // Create an equity asset associated with the issuer
        Asset asset = new Asset(
                "Apple",
                AssetClass.EQUITY,
                apple
        );

        // Persist the asset
        Asset savedAsset = assetRepository.save(asset);

        // Verify that the database generated an ID
        assertNotNull(savedAsset.getId());

        // Retrieve the asset from the repository
        Optional<Asset> foundAsset = assetRepository.findById(savedAsset.getId());

        // Verify that the asset was found
        assertTrue(foundAsset.isPresent());

        // Verify that the retrieved asset has the expected ID
        assertEquals(
            savedAsset.getId(),
            foundAsset.get().getId()
        );

        assertEquals(
                AssetClass.EQUITY,
                foundAsset.get().getAssetClass()
        );

        assertEquals(
            "Apple",
            foundAsset.get().getName()
        );
        
        assertNotNull(foundAsset.get().getIssuer());

        assertNotNull(
                foundAsset.get().getIssuer().getId()
        );

        assertEquals(
                "Apple Inc.",
                foundAsset.get().getIssuer().getName()
        );
        
        assertEquals(
                "United States",
                foundAsset.get().getIssuer().getCountry()
        );
    }


    @Test
    void shouldFindAllAssets() {
        Issuer apple = new Issuer(
            "Apple Inc.",
            "United States"
        );
        issuerRepository.save(apple);

        Issuer tesla = new Issuer(
                "Tesla Inc.",
                "United States"
        );
        issuerRepository.save(tesla);

        Asset appleAsset = new Asset(
            "Apple",
            AssetClass.EQUITY,
            apple
        );

        Asset teslaAsset = new Asset(
            "Tesla",
            AssetClass.EQUITY,
            tesla
        );

        Asset goldAsset = new Asset(
            "Gold",
            AssetClass.COMMODITY,
            null
        );

        assetRepository.save(appleAsset);
        assetRepository.save(teslaAsset);
        assetRepository.save(goldAsset);
    
        List<Asset> assets = assetRepository.findAll();
    
        assertEquals(3, assets.size());
    }



    @Test
    void shouldFindAssetsByAssetClass() {

        Issuer apple = new Issuer(
            "Apple Inc.",
            "United States"
        );
        issuerRepository.save(apple);

        Issuer tesla = new Issuer(
                "Tesla Inc.",
                "United States"
        );
        issuerRepository.save(tesla);

        Asset appleAsset = new Asset(
            "Apple",
            AssetClass.EQUITY,
            apple
        );

        Asset teslaAsset = new Asset(
            "Tesla",
            AssetClass.EQUITY,
            tesla
        );

        Asset goldAsset = new Asset(
            "Gold",
            AssetClass.COMMODITY,
            null
        );

        assetRepository.save(appleAsset);
        assetRepository.save(teslaAsset);
        assetRepository.save(goldAsset);
        
        List<Asset> equities =
            assetRepository.findByAssetClass(AssetClass.EQUITY);
        
        List<Asset> commodities =
            assetRepository.findByAssetClass(AssetClass.COMMODITY);
            
        assertEquals(2, equities.size());
        assertEquals(1, commodities.size());

        assertEquals("Apple", 
            equities.get(0).getName()
        );
        
        assertEquals("Tesla", 
            equities.get(1).getName()
        );
        
        assertEquals("Gold", commodities.get(0).getName());
    }


    @Test
    void shouldFindAssetByIdentifier() {
        Issuer apple = issuerRepository.save(
                new Issuer("Apple Inc.", "United States"));
        Asset appleAsset = assetRepository.save(
                new Asset("Apple", AssetClass.EQUITY, apple));
        assetIdentifierRepository.save(new AssetIdentifier(
                appleAsset, IdentifierScheme.TWELVE_DATA_SYMBOL, "AAPL"));

        Optional<Asset> asset = assetRepository.findByIdentifier(
                IdentifierScheme.TWELVE_DATA_SYMBOL, "AAPL");

        assertTrue(asset.isPresent());
        assertEquals("Apple", asset.get().getName());
    }

    @Test
    void shouldReturnEmptyForUnknownIdentifier() {
        Optional<Asset> asset = assetRepository.findByIdentifier(
                IdentifierScheme.TWELVE_DATA_SYMBOL, "NOPE");

        assertTrue(asset.isEmpty());
    }

    @Test
    void shouldRejectSameIdentifierOnDifferentAssets() {
        Issuer apple = issuerRepository.save(
                new Issuer("Apple Inc.", "United States"));
        Issuer other = issuerRepository.save(
                new Issuer("Other Inc.", "United States"));
        Asset first = assetRepository.save(
                new Asset("Apple", AssetClass.EQUITY, apple));
        Asset second = assetRepository.save(
                new Asset("Other", AssetClass.EQUITY, other));

        assetIdentifierRepository.saveAndFlush(new AssetIdentifier(
                first, IdentifierScheme.TWELVE_DATA_SYMBOL, "AAPL"));

        assertThrows(DataIntegrityViolationException.class, () ->
                assetIdentifierRepository.saveAndFlush(new AssetIdentifier(
                        second, IdentifierScheme.TWELVE_DATA_SYMBOL, "AAPL")));
    }
}