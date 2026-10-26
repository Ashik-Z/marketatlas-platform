package com.marketatlas.market.dto;

import com.marketatlas.enums.AssetClass;
import com.marketatlas.market.entity.Asset;

public record AssetResponse(
        Long id,
        String name,
        AssetClass assetClass,
        Long issuerId,
        String symbol,
        String currency,
        String unit
) {

    public static AssetResponse from(Asset asset, String symbol) {
        return new AssetResponse(
                asset.getId(),
                asset.getName(),
                asset.getAssetClass(),
                asset.getIssuer() == null ? null : asset.getIssuer().getId(),
                symbol,
                asset.getCurrency(),
                asset.getUnit()
        );
    }
}