package com.marketatlas.market.repository;

import com.marketatlas.market.entity.Asset;
import com.marketatlas.market.entity.MarketData;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

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

    @Modifying
    @Query(value = """
        INSERT INTO market_data (
            asset_id, timestamp, open, high, low, close, volume
        )
        VALUES (
            :assetId, :timestamp, :open, :high, :low, :close, :volume
        )
        ON CONFLICT (asset_id, timestamp)
        DO UPDATE SET
            open = EXCLUDED.open,
            high = EXCLUDED.high,
            low = EXCLUDED.low,
            close = EXCLUDED.close,
            volume = EXCLUDED.volume
        """, nativeQuery = true)
    int upsertMarketData(
            @Param("assetId") Long assetId,
            @Param("timestamp") LocalDate timestamp,
            @Param("open") Double open,
            @Param("high") Double high,
            @Param("low") Double low,
            @Param("close") Double close,
            @Param("volume") Double volume
    );
}