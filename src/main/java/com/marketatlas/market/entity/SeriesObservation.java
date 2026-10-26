package com.marketatlas.market.entity;

import jakarta.persistence.*;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

@Entity
@Table(name = "series_observation")
public class SeriesObservation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "series_id", nullable = false)
    private DataSeries series;

    @Column(nullable = false)
    private LocalDate observationDate;

    /** Null means the provider reported this date as missing; never zero. */
    @Column(precision = 20, scale = 6)
    private BigDecimal value;

    @Column(nullable = false)
    private Instant fetchedAt;

    protected SeriesObservation() {
    }

    public SeriesObservation(DataSeries series, LocalDate observationDate,
                             BigDecimal value, Instant fetchedAt) {
        this.series = series;
        this.observationDate = observationDate;
        this.value = value;
        this.fetchedAt = fetchedAt;
    }

    public Long getId() { return id; }
    public DataSeries getSeries() { return series; }
    public LocalDate getObservationDate() { return observationDate; }
    public BigDecimal getValue() { return value; }
    public Instant getFetchedAt() { return fetchedAt; }
}