package com.marketatlas.market.service;

import com.marketatlas.market.entity.Asset;
import com.marketatlas.market.entity.MarketData;
import com.marketatlas.market.dto.MarketDataHistoryResponseDTO;

import java.util.List;
import java.util.Optional;
import java.time.LocalDate;

public interface MarketDataService {
    MarketData createMarketData(MarketData marketData);
    Optional<MarketData> findMarketDataById(Long id);
    List<MarketData> findMarketDataByAsset(Asset asset);    
    List<MarketDataHistoryResponseDTO> fetchHistoricalData(
        String symbol,
        LocalDate startDate,
        LocalDate endDate,
        String interval
    );
}
