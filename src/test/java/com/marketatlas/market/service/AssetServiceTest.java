package com.marketatlas.market.service;

import com.marketatlas.market.repository.AssetRepository;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.marketatlas.enums.AssetClass;
import com.marketatlas.market.entity.Asset;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.extension.ExtendWith;

@ExtendWith(MockitoExtension.class)
class AssetServiceTest {

    @Mock
    private AssetRepository assetRepository;

    private AssetServiceImpl assetService;

    @BeforeEach
    void setUp() {
        assetService = new AssetServiceImpl(assetRepository);
    }

    @Test
    void shouldCreateAsset() {

        Asset asset = new Asset(
                "Apple",
                "AAPL",
                AssetClass.EQUITY,
                null
        );
        
        when(assetRepository.save(asset)).thenReturn(asset);
    
        Asset savedAsset = assetService.createAsset(asset);
    
        assertEquals(asset, savedAsset);
    
        verify(assetRepository).save(asset);
    }

    @Test
    void shouldFindAssetsByClass() {
        Asset apple = new Asset(
                "Apple",
                "AAPL",
                AssetClass.EQUITY,
                null
        );

        Asset tesla = new Asset(
                "Tesla",
                "TSLA",
                AssetClass.EQUITY,
                null
            );
        
        List<Asset> assets = List.of(apple, tesla);
        
        when(assetRepository.findByAssetClass(AssetClass.EQUITY))
            .thenReturn(assets);
    
        List<Asset> result =
            assetService.findAssetsByClass(AssetClass.EQUITY);
    
        assertEquals(2, result.size());
        assertEquals("Apple", result.get(0).getName());
        assertEquals("Tesla", result.get(1).getName());
    
        verify(assetRepository)
            .findByAssetClass(AssetClass.EQUITY);
    }

    @Test
    void shouldFindAssetById() {
    
        Asset apple = new Asset(
            "Apple",
            "AAPL",
            AssetClass.EQUITY,
            null
        );
    
        Long id = 1L;
    
        when(assetRepository.findById(id))
            .thenReturn(Optional.of(apple));
    
        Optional<Asset> result =
            assetService.findAssetById(id);
    
        assertTrue(result.isPresent());
        assertEquals(apple, result.get());
    
        verify(assetRepository).findById(id);
    }

    @Test
    void shouldFindAssetBySymbol() {
        Asset apple = new Asset(
            "Apple",
            "AAPL",
            AssetClass.EQUITY,
            null
        );
    
        when(assetRepository.findBySymbol("AAPL"))
            .thenReturn(Optional.of(apple));
    
        Optional<Asset> result =
            assetService.findAssetBySymbol("AAPL");
    
        assertTrue(result.isPresent());
        assertEquals(apple, result.get());
    
        verify(assetRepository).findBySymbol("AAPL");
    }
}