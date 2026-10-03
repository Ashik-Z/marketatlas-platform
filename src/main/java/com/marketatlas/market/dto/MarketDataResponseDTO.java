package com.marketatlas.market.dto;

import java.util.List;

public class MarketDataResponseDTO {
    private String symbol;
    private String interval;
    private String currency;
    private String exchange;
    private List<MarketDataDTO> values;
    private String status;

    public MarketDataResponseDTO(String symbol, String interval, String currency, String exchange, List<MarketDataDTO> values, String status) {
        this.symbol = symbol;
        this.interval = interval;
        this.currency = currency;
        this.exchange = exchange;
        this.values = values;
        this.status = status;
    }

    public String getSymbol() {
        return symbol;
    }

    public String getInterval() {
        return interval;
    }

    public String getCurrency() {
        return currency;
    }

    public String getExchange() {
        return exchange;
    }

    public List<MarketDataDTO> getValues() {
        return values;
    }

    public String getStatus() {
        return status;
    }
    
}