package com.marketatlas.market.service;
import com.marketatlas.market.entity.Asset;
import com.marketatlas.enums.AssetClass;

import java.util.Optional;
import java.util.Collection;
import java.util.Map;
import java.util.List;

public interface AssetService {
    Asset createAsset(String name, AssetClass assetClass, Long issuerId, String symbol);
    List<Asset> findAssetsByClass(AssetClass assetClass);
    List<Asset> findAllAssets();
    Map<Long, String> findSymbolsByAssetIds(Collection<Long> assetIds);
    List<String> findAllProviderSymbols();
    Optional<Asset> findAssetById(Long id);
    Optional<Asset> findAssetBySymbol(String symbol);
}
