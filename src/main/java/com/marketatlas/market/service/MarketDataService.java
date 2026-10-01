package com.marketatlas.market.service;

import org.springframework.stereotype.Service;

import com.marketatlas.market.entity.Asset;
import com.marketatlas.market.entity.MarketData;

import java.util.List;
import java.util.Optional;

@Service
public interface MarketDataService {
    MarketData createMarketData(MarketData marketData);
    Optional<MarketData> findMarketDataById(Long id);
    List<MarketData> findMarketDataByAsset(Asset asset);    
}
