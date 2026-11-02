package com.marketatlas.watchlist.repository;

import com.marketatlas.watchlist.model.WatchlistItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface WatchlistItemRepository extends JpaRepository<WatchlistItem, Long> {
    List<WatchlistItem> findByOwnerUsernameOrderByAddedAtDesc(String ownerUsername);
    boolean existsByOwnerUsernameAndAssetId(String ownerUsername, Long assetId);
    long countByOwnerUsername(String ownerUsername);
    long deleteByOwnerUsernameAndAssetId(String ownerUsername, Long assetId);

    @Modifying
    @Query(value = """
        INSERT INTO watchlist_item (owner_username, asset_id, added_at)
        VALUES (:owner, :assetId, now())
        ON CONFLICT (owner_username, asset_id) DO NOTHING
        """, nativeQuery = true)
    int insertIfAbsent(@Param("owner") String owner, @Param("assetId") Long assetId);
}
