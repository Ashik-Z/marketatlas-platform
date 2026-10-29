package com.marketatlas.market.repository;

import com.marketatlas.market.entity.DataSeries;
import com.marketatlas.market.entity.SeriesObservation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

public interface SeriesObservationRepository extends JpaRepository<SeriesObservation, Long> {

    List<SeriesObservation> findBySeriesAndObservationDateBetweenOrderByObservationDateAsc(
            DataSeries series, LocalDate from, LocalDate to);
    List<SeriesObservation> findBySeriesOrderByObservationDateAsc(DataSeries series);

    @Modifying
    @Query(value = """
        INSERT INTO series_observation (series_id, observation_date, value, fetched_at)
        SELECT CAST(:seriesId AS bigint), u.d, u.v, CAST(:fetchedAt AS timestamptz)
        FROM unnest(
            CAST(:dates AS date[]),
            CAST(:values AS numeric[])
        ) AS u(d, v)
        ORDER BY u.d
        ON CONFLICT (series_id, observation_date)
        DO UPDATE SET value = EXCLUDED.value, fetched_at = EXCLUDED.fetched_at
        """, nativeQuery = true)
    int upsertBatch(
            @Param("seriesId") Long seriesId,
            @Param("dates") LocalDate[] dates,
            @Param("values") BigDecimal[] values,
            @Param("fetchedAt") Instant fetchedAt
    );
}