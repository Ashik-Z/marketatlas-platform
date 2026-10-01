package com.marketatlas.market.repository;

import com.marketatlas.market.entity.Asset;
import com.marketatlas.market.entity.MarketData;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface MarketDataRepository extends JpaRepository<MarketData, Long> {
    List<MarketData> findByAsset(Asset asset);
}