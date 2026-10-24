package com.marketatlas.market.service;

import com.marketatlas.market.entity.Asset;
import com.marketatlas.enums.AssetClass;
import com.marketatlas.market.repository.AssetRepository;
import org.springframework.stereotype.Service;
import com.marketatlas.enums.IdentifierScheme;

import java.util.List;
import java.util.Optional;

@Service 
public class AssetServiceImpl implements AssetService {

    private final AssetRepository assetRepository;
    
    public AssetServiceImpl(AssetRepository assetRepository) {
        this.assetRepository = assetRepository;
    }

    @Override
    public Asset createAsset(Asset asset) {
        return assetRepository.save(asset);
    }

    @Override
    public List<Asset> findAssetsByClass(AssetClass assetClass) {
        return assetRepository.findByAssetClass(assetClass);
    }

    @Override
    public Optional<Asset> findAssetById(Long id) {
        return assetRepository.findById(id);
    }

    @Override
    public Optional<Asset> findAssetBySymbol(String symbol) {
        return assetRepository.findByIdentifier(
                IdentifierScheme.TWELVE_DATA_SYMBOL,
                symbol
        );
    }
}