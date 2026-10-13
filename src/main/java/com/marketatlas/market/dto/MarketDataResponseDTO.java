package com.marketatlas.market.dto;

import java.util.List;

public class MarketDataResponseDTO {
    private String symbol;
    private String interval;
    private String currency;
    private String exchange;
    private List<MarketDataDTO> values;
    private String status;
    private Integer code;
    private String message;

    public MarketDataResponseDTO() {
    }

    public MarketDataResponseDTO(
        String symbol, 
        String interval, 
        String currency, 
        String exchange, 
        List<MarketDataDTO> values, 
        String status, 
        Integer code, 
        String message
    ) {
        this.symbol = symbol;
        this.interval = interval;
        this.currency = currency;
        this.exchange = exchange;
        this.values = values;
        this.status = status;
        this.code = code;
        this.message = message;
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

    public Integer getCode() {
        return code;
    }
    
    public String getMessage() {
        return message;
    }
    
}