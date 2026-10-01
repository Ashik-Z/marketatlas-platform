package com.marketatlas.market.validation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

import com.marketatlas.enums.AssetClass;
import com.marketatlas.market.entity.Asset;

public class AssetValidator implements ConstraintValidator<ValidAsset, Asset> {
    @Override
    public boolean isValid(Asset asset, ConstraintValidatorContext context) {
        if (asset == null) {
            return false;
        } 
        if (asset.getAssetClass() == AssetClass.EQUITY) {
            return asset.getIssuer() != null;
        }
        if (asset.getAssetClass() == AssetClass.COMMODITY) {
            return asset.getIssuer() == null;
        }
        return true;
    }
}
