package com.marketatlas.market.dto;


import java.time.LocalDateTime;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.LocalDateTime;


public class MarketDataDTO {
    private Double open;
    private Double high;
    private Double low;
    private Double close;
    private Double volume;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @JsonProperty("datetime")
    private LocalDateTime timestamp;

    public MarketDataDTO(Double open, Double high, Double low, Double close, Double volume, LocalDateTime timestamp) {
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

    public LocalDateTime getTimestamp() {
        return timestamp;
    }
}