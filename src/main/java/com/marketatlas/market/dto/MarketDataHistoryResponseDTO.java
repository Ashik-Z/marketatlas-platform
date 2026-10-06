package com.marketatlas.market.dto;

import java.time.LocalDate;


public class MarketDataHistoryResponseDTO {
    private String symbol;
    private Double open;
    private Double high;
    private Double low;
    private Double close;
    private Double volume;
    private LocalDate timestamp;

    public MarketDataHistoryResponseDTO(String symbol, Double open, Double high, Double low, Double close, Double volume, LocalDate timestamp) {
        this.symbol = symbol;
        this.open = open;
        this.high = high;
        this.low = low;
        this.close = close;
        this.volume = volume;
        this.timestamp = timestamp;
    }

    public String getSymbol() {
        return symbol;
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

    public LocalDate getTimestamp() {
        return timestamp;
    }
    
}