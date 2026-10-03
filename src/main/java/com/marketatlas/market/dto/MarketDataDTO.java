package com.marketatlas.market.dto;


import java.time.LocalDate;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;


public class MarketDataDTO {
    private Double open;
    private Double high;
    private Double low;
    private Double close;
    private Double volume;

    @JsonFormat(pattern = "yyyy-MM-dd")
    @JsonProperty("datetime")
    private LocalDate timestamp;

    public MarketDataDTO(Double open, Double high, Double low, Double close, Double volume, LocalDate timestamp) {
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

    public LocalDate getTimestamp() {
        return timestamp;
    }
}