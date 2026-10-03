package com.marketatlas.market.service;

import com.marketatlas.market.entity.Asset;
import com.marketatlas.market.entity.MarketData;
import com.marketatlas.market.dto.MarketDataDTO;

import com.marketatlas.market.repository.MarketDataRepository;
import com.marketatlas.market.client.MarketDataClient;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;
import java.time.LocalDate;
import java.util.ArrayList;


@Service
public class MarketDataServiceImpl implements MarketDataService {

    private final MarketDataRepository marketDataRepository;
    private final AssetService assetService;
    private final MarketDataClient marketDataClient;

    public MarketDataServiceImpl(MarketDataRepository marketDataRepository, AssetService assetService, MarketDataClient marketDataClient) {
        this.marketDataRepository = marketDataRepository;
        this.assetService = assetService;
        this.marketDataClient = marketDataClient;
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

    @Override
    public List<MarketData> fetchHistoricalData(String symbol, LocalDate startDate, LocalDate endDate, String interval) {
        Asset asset = assetService.findAssetBySymbol(symbol)
                .orElseThrow(() -> new IllegalArgumentException(
                        "Asset not found: " + symbol
                ));

        List<MarketDataDTO> marketDataDTOs =
            marketDataClient.getHistoricalData(
                symbol,
                startDate,
                endDate,
                interval
            );

        List<MarketData> marketDataList = new ArrayList<>();
        
        for (MarketDataDTO dto : marketDataDTOs) {
            MarketData marketData = new MarketData(
                dto.getOpen(),
                dto.getHigh(),
                dto.getLow(),
                dto.getClose(),
                dto.getVolume(),
                asset,
                dto.getTimestamp()
            );
        
            marketDataList.add(marketData);
        }

        return marketDataRepository.saveAll(marketDataList);
    }
}