package com.marketatlas.market.repository;

import com.marketatlas.market.entity.AssetIdentifier;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface AssetIdentifierRepository
        extends JpaRepository<AssetIdentifier, Long> {
}