package com.marketatlas.market.repository;

import com.marketatlas.market.entity.Asset;
import com.marketatlas.market.entity.MarketData;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.time.LocalDate;
import java.util.Collection;

@Repository
public interface MarketDataRepository extends JpaRepository<MarketData, Long> {
    List<MarketData> findByAsset(Asset asset);
    Optional<MarketData> findByAssetAndTimestamp(
        Asset asset,
        LocalDate timestamp
    );
    List<MarketData> findByAssetAndTimestampIn(
            Asset asset,
            Collection<LocalDate> timestamps
    );
}