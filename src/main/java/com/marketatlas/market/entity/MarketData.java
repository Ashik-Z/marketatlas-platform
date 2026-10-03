package com.marketatlas.market.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;


@Entity 
@Table(name = "market_data")
public class MarketData {
    @Id 
    @GeneratedValue (strategy=GenerationType.IDENTITY)
    private Long id;
    private Double open;
    private Double high;
    private Double low;
    private Double close;
    private Double volume;

    @NotNull
    @ManyToOne
    @JoinColumn(name = "asset_id")
    private Asset asset;
    
    @NotNull 
    @Column (nullable=false)
    private LocalDate timestamp;

    public MarketData(Double open, Double high, Double low, Double close, Double volume, Asset asset, LocalDate timestamp) {
        this.open = open;
        this.high = high;
        this.low = low;
        this.close = close;
        this.volume = volume;
        this.asset = asset;
        this.timestamp = timestamp;
    }

    protected MarketData() {
        
    }

    public Long getId() {
        return id;
    }

    public Double getOpen() {
        return open;
    }

    public Double getHigh() {
        return high;
    }

    public Double getLow() {
        return low;
    }

    public Double getClose() {
        return close;
    }

    public Double getVolume() {
        return volume;
    }

    public Asset getAsset() {
        return asset;
    }

    public LocalDate getTimestamp() {
        return timestamp;
    }
}