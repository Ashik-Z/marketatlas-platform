package com.marketatlas.market.repository;

import com.marketatlas.enums.AssetClass;
import com.marketatlas.enums.IdentifierScheme;
import com.marketatlas.market.entity.Asset;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.Lock;

import java.util.List;
import java.util.Optional;

@Repository
public interface AssetRepository extends JpaRepository<Asset, Long> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select a from Asset a where a.id = :id")
    Optional<Asset> findByIdForUpdate(@Param("id") Long id);
    List<Asset> findByAssetClass(AssetClass assetClass);

    @Query("""
        select a
        from AssetIdentifier i
        join i.asset a
        where i.scheme = :scheme
          and i.identifierValue = :identifierValue
        """)
    Optional<Asset> findByIdentifier(
            @Param("scheme") IdentifierScheme scheme,
            @Param("identifierValue") String identifierValue
    );
}