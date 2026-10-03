package com.marketatlas.market.repository;

import com.marketatlas.market.entity.Asset;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import com.marketatlas.enums.AssetClass;

@Repository 
public interface AssetRepository extends JpaRepository<Asset, Long> {
    List<Asset> findByAssetClass(AssetClass assetClass);
    Optional<Asset> findBySymbol(String symbol);
}