package com.marketatlas.market.client;

import com.marketatlas.market.dto.MarketDataDTO;

import java.time.LocalDate;
import java.util.List;

public interface MarketDataClient {
    List<MarketDataDTO> getMarketData(
        String symbol,
        LocalDate startDate,
        LocalDate endDate,
        String interval
    );
}