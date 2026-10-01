package com.marketatlas.market.service;

import com.marketatlas.market.entity.Asset;
import com.marketatlas.market.entity.MarketData;
import com.marketatlas.market.repository.MarketDataRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class MarketDataServiceImpl implements MarketDataService {

    private final MarketDataRepository marketDataRepository;

    public MarketDataServiceImpl(MarketDataRepository marketDataRepository) {
        this.marketDataRepository = marketDataRepository;
    }

    @Override
    public MarketData createMarketData(MarketData marketData) {
        return marketDataRepository.save(marketData);
    }

    @Override
    public List<MarketData> findMarketDataByAsset(Asset asset) {
        return marketDataRepository.findByAsset(asset);
    }

    @Override
    public Optional<MarketData> findMarketDataById(Long id) {
        return marketDataRepository.findById(id);
    }
}