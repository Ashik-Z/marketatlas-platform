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
    List<MarketData> findByAssetAndTimestampBetweenOrderByTimestampAsc(
            Asset asset,
            LocalDate from,
            LocalDate to
    );

    @Modifying
    @Query(value = """
        INSERT INTO market_data (
            asset_id, timestamp, open, high, low, close, volume
        )
        SELECT CAST(:assetId AS bigint), u.ts, u.o, u.h, u.l, u.c, u.v
        FROM unnest(
            CAST(:timestamps AS date[]),
            CAST(:opens AS double precision[]),
            CAST(:highs AS double precision[]),
            CAST(:lows AS double precision[]),
            CAST(:closes AS double precision[]),
            CAST(:volumes AS double precision[])
        ) AS u(ts, o, h, l, c, v)
        ORDER BY u.ts
        ON CONFLICT (asset_id, timestamp)
        DO UPDATE SET
            open = EXCLUDED.open,
            high = EXCLUDED.high,
            low = EXCLUDED.low,
            close = EXCLUDED.close,
            volume = EXCLUDED.volume
        """, nativeQuery = true)
    int upsertBatch(
            @Param("assetId") Long assetId,
            @Param("timestamps") LocalDate[] timestamps,
            @Param("opens") Double[] opens,
            @Param("highs") Double[] highs,
            @Param("lows") Double[] lows,
            @Param("closes") Double[] closes,
            @Param("volumes") Double[] volumes
    );
}