package com.marketatlas.market.dto;


import java.time.Instant;


public class MarketDataDTO {
    private Double open;
    private Double high;
    private Double low;
    private Double close;
    private Double volume;

    private Instant timestamp;

    public MarketDataDTO(Double open, Double high, Double low, Double close, Double volume, Instant timestamp) {
        this.open = open;
        this.high = high;
        this.low = low;
        this.close = close;
        this.volume = volume;
        this.timestamp = timestamp;
    }

    protected MarketDataDTO() {
        
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

    public Instant getTimestamp() {
        return timestamp;
    }
}