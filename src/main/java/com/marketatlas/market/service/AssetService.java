package com.marketatlas.market.service;
import com.marketatlas.market.entity.Asset;
import com.marketatlas.enums.AssetClass;

import java.util.Optional;

import java.util.List;

public interface AssetService {
    Asset createAsset(Asset asset);
    List<Asset> findAssetsByClass(AssetClass assetClass);
    Optional<Asset> findAssetById(Long id);
    Optional<Asset> findAssetBySymbol(String symbol);
}
