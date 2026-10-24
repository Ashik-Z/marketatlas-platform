package com.marketatlas.market.service;

import com.marketatlas.common.exception.DuplicateResourceException;
import com.marketatlas.common.exception.ResourceNotFoundException;
import com.marketatlas.enums.AssetClass;
import com.marketatlas.enums.IdentifierScheme;
import com.marketatlas.market.entity.Asset;
import com.marketatlas.market.entity.AssetIdentifier;
import com.marketatlas.market.entity.Issuer;
import com.marketatlas.market.repository.AssetIdentifierRepository;
import com.marketatlas.market.repository.AssetRepository;
import com.marketatlas.market.repository.IssuerRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AssetServiceTest {

    @Mock
    private AssetRepository assetRepository;

    @Mock
    private IssuerRepository issuerRepository;

    @Mock
    private AssetIdentifierRepository assetIdentifierRepository;

    private AssetServiceImpl assetService;

    @BeforeEach
    void setUp() {
        assetService = new AssetServiceImpl(
                assetRepository, issuerRepository, assetIdentifierRepository);
    }

    @Test
    void shouldCreateEquityWithNormalizedSymbol() {
        Issuer apple = new Issuer("Apple Inc.", "United States");
        when(issuerRepository.findById(1L)).thenReturn(Optional.of(apple));
        when(assetRepository.save(any(Asset.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        Asset result = assetService.createAsset(
                "  Apple  ", AssetClass.EQUITY, 1L, " aapl ");

        assertEquals("Apple", result.getName());
        assertEquals(apple, result.getIssuer());

        ArgumentCaptor<AssetIdentifier> captor =
                ArgumentCaptor.forClass(AssetIdentifier.class);
        verify(assetIdentifierRepository).saveAndFlush(captor.capture());
        assertEquals(IdentifierScheme.TWELVE_DATA_SYMBOL, captor.getValue().getScheme());
        assertEquals("AAPL", captor.getValue().getIdentifierValue());
    }

    @Test
    void shouldCreateCommodityWithoutSymbolAndSkipIdentifier() {
        when(assetRepository.save(any(Asset.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        Asset result = assetService.createAsset(
                "Gold", AssetClass.COMMODITY, null, "  ");

        assertEquals("Gold", result.getName());
        verifyNoInteractions(issuerRepository, assetIdentifierRepository);
    }

    @Test
    void shouldRejectEquityWithoutIssuer() {
        assertThrows(IllegalArgumentException.class, () ->
                assetService.createAsset("Apple", AssetClass.EQUITY, null, "AAPL"));

        verifyNoInteractions(assetRepository, assetIdentifierRepository);
    }

    @Test
    void shouldRejectCommodityWithIssuer() {
        assertThrows(IllegalArgumentException.class, () ->
                assetService.createAsset("Gold", AssetClass.COMMODITY, 1L, null));

        verifyNoInteractions(assetRepository, issuerRepository, assetIdentifierRepository);
    }

    @Test
    void shouldRejectUnknownIssuer() {
        when(issuerRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () ->
                assetService.createAsset("Apple", AssetClass.EQUITY, 99L, "AAPL"));

        verify(assetRepository, never()).save(any(Asset.class));
    }

    @Test
    void shouldRejectDuplicateSymbolBeforeSaving() {
        Issuer apple = new Issuer("Apple Inc.", "United States");
        when(issuerRepository.findById(1L)).thenReturn(Optional.of(apple));
        when(assetRepository.findByIdentifier(IdentifierScheme.TWELVE_DATA_SYMBOL, "AAPL"))
                .thenReturn(Optional.of(new Asset("Apple", AssetClass.EQUITY, apple)));

        assertThrows(DuplicateResourceException.class, () ->
                assetService.createAsset("Apple", AssetClass.EQUITY, 1L, "aapl"));

        verify(assetRepository, never()).save(any(Asset.class));
    }

    @Test
    void shouldReportDuplicateWhenConstraintFiresAfterCheck() {
        Issuer apple = new Issuer("Apple Inc.", "United States");
        when(issuerRepository.findById(1L)).thenReturn(Optional.of(apple));
        when(assetRepository.save(any(Asset.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        when(assetIdentifierRepository.saveAndFlush(any(AssetIdentifier.class)))
                .thenThrow(new DataIntegrityViolationException("uk_asset_identifier_scheme_value"));

        assertThrows(DuplicateResourceException.class, () ->
                assetService.createAsset("Apple", AssetClass.EQUITY, 1L, "AAPL"));
    }

    @Test
    void shouldFindAssetsByClass() {
        Asset apple = new Asset("Apple", AssetClass.EQUITY, null);
        Asset tesla = new Asset("Tesla", AssetClass.EQUITY, null);
        when(assetRepository.findByAssetClass(AssetClass.EQUITY))
                .thenReturn(List.of(apple, tesla));

        List<Asset> result = assetService.findAssetsByClass(AssetClass.EQUITY);

        assertEquals(2, result.size());
        assertEquals("Apple", result.get(0).getName());
        assertEquals("Tesla", result.get(1).getName());
    }

    @Test
    void shouldFindAssetById() {
        Asset apple = new Asset("Apple", AssetClass.EQUITY, null);
        when(assetRepository.findById(1L)).thenReturn(Optional.of(apple));

        Optional<Asset> result = assetService.findAssetById(1L);

        assertTrue(result.isPresent());
        assertEquals(apple, result.get());
    }

    @Test
    void shouldFindAssetBySymbol() {
        Asset apple = new Asset("Apple", AssetClass.EQUITY, null);
        when(assetRepository.findByIdentifier(IdentifierScheme.TWELVE_DATA_SYMBOL, "AAPL"))
                .thenReturn(Optional.of(apple));

        Optional<Asset> result = assetService.findAssetBySymbol("AAPL");

        assertTrue(result.isPresent());
        assertEquals(apple, result.get());
    }
}