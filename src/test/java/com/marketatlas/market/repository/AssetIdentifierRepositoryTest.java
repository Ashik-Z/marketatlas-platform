package com.marketatlas.market.repository;

import com.marketatlas.enums.AssetClass;
import com.marketatlas.enums.IdentifierScheme;
import com.marketatlas.market.TestPostgresConfiguration;
import com.marketatlas.market.entity.Asset;
import com.marketatlas.market.entity.AssetIdentifier;
import com.marketatlas.market.entity.Issuer;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.context.annotation.Import;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import(TestPostgresConfiguration.class)
class AssetIdentifierRepositoryTest {

    @Autowired
    private AssetIdentifierRepository identifierRepository;

    @Autowired
    private AssetRepository assetRepository;

    @Autowired
    private IssuerRepository issuerRepository;

    private Asset equity(String name, String issuerName) {
        Issuer issuer = issuerRepository.save(new Issuer(issuerName, "United States"));
        return assetRepository.save(new Asset(name, AssetClass.EQUITY, issuer));
    }

    @Test
    void shouldListSymbolsSortedAndSkipAssetsWithoutOne() {
        Asset microsoft = equity("Microsoft", "Microsoft Corp.");
        Asset apple = equity("Apple", "Apple Inc.");
        assetRepository.save(new Asset("Gold", AssetClass.COMMODITY, null));

        identifierRepository.save(
                new AssetIdentifier(microsoft, IdentifierScheme.TWELVE_DATA_SYMBOL, "MSFT"));
        identifierRepository.save(
                new AssetIdentifier(apple, IdentifierScheme.TWELVE_DATA_SYMBOL, "AAPL"));

        assertEquals(
                List.of("AAPL", "MSFT"),
                identifierRepository.findIdentifierValuesByScheme(
                        IdentifierScheme.TWELVE_DATA_SYMBOL));
    }

    @Test
    void shouldReturnNoSymbolsWhenNoneExist() {
        assertTrue(identifierRepository
                .findIdentifierValuesByScheme(IdentifierScheme.TWELVE_DATA_SYMBOL)
                .isEmpty());
    }

    @Test
    void shouldFindIdentifiersOnlyForTheGivenAssets() {
        Asset apple = equity("Apple", "Apple Inc.");
        Asset microsoft = equity("Microsoft", "Microsoft Corp.");

        identifierRepository.save(
                new AssetIdentifier(apple, IdentifierScheme.TWELVE_DATA_SYMBOL, "AAPL"));
        identifierRepository.save(
                new AssetIdentifier(microsoft, IdentifierScheme.TWELVE_DATA_SYMBOL, "MSFT"));

        List<AssetIdentifier> found = identifierRepository.findByAssetIdsAndScheme(
                List.of(apple.getId()), IdentifierScheme.TWELVE_DATA_SYMBOL);

        assertEquals(1, found.size());
        assertEquals("AAPL", found.get(0).getIdentifierValue());
    }
}