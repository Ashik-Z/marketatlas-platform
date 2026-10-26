package com.marketatlas.market.repository;

import com.marketatlas.enums.SeriesProvider;
import com.marketatlas.market.entity.DataSeries;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface DataSeriesRepository extends JpaRepository<DataSeries, Long> {

    Optional<DataSeries> findByProviderAndExternalId(SeriesProvider provider, String externalId);
}