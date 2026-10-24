package com.marketatlas.market.repository;

import com.marketatlas.market.entity.MarketDataCoverage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface MarketDataCoverageRepository
        extends JpaRepository<MarketDataCoverage, Long> {

    @Query("""
        select c from MarketDataCoverage c
        where c.asset.id = :assetId
          and c.endDate >= :from
          and c.startDate <= :to
        order by c.startDate
        """)
    List<MarketDataCoverage> findOverlapping(
            @Param("assetId") Long assetId,
            @Param("from") LocalDate from,
            @Param("to") LocalDate to);
}