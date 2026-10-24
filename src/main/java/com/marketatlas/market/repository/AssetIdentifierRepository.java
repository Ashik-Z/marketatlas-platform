package com.marketatlas.market.repository;

import com.marketatlas.enums.IdentifierScheme;
import com.marketatlas.market.entity.AssetIdentifier;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;

@Repository
public interface AssetIdentifierRepository
        extends JpaRepository<AssetIdentifier, Long> {

    @Query("""
        select i from AssetIdentifier i
        where i.asset.id in :assetIds and i.scheme = :scheme
        """)
    List<AssetIdentifier> findByAssetIdsAndScheme(
            @Param("assetIds") Collection<Long> assetIds,
            @Param("scheme") IdentifierScheme scheme);
}