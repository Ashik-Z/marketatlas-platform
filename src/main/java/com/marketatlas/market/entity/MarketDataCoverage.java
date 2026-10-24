package com.marketatlas.market.entity;

import jakarta.persistence.*;

import java.time.Instant;
import java.time.LocalDate;

@Entity
@Table(name = "market_data_coverage")
public class MarketDataCoverage {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "asset_id", nullable = false)
    private Asset asset;

    @Column(name = "start_date", nullable = false)
    private LocalDate startDate;

    @Column(name = "end_date", nullable = false)
    private LocalDate endDate;

    @Column(name = "fetched_at", nullable = false)
    private Instant fetchedAt;

    public MarketDataCoverage(Asset asset, LocalDate startDate, LocalDate endDate) {
        if (startDate.isAfter(endDate)) {
            throw new IllegalArgumentException(
                    "Coverage start must be before or equal to its end.");
        }
        this.asset = asset;
        this.startDate = startDate;
        this.endDate = endDate;
        this.fetchedAt = Instant.now();
    }

    protected MarketDataCoverage() {
    }

    public Long getId() {
        return id;
    }

    public Asset getAsset() {
        return asset;
    }

    public LocalDate getStartDate() {
        return startDate;
    }

    public LocalDate getEndDate() {
        return endDate;
    }

    public Instant getFetchedAt() {
        return fetchedAt;
    }
}