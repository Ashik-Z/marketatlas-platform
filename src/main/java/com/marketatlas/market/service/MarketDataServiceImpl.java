package com.marketatlas.market.service;

import com.marketatlas.market.entity.Asset;
import com.marketatlas.market.entity.MarketData;
import com.marketatlas.market.dto.MarketDataDTO;

import com.marketatlas.market.dto.MarketDataHistoryResponseDTO; 
import com.marketatlas.market.repository.MarketDataRepository;
import com.marketatlas.market.client.MarketDataClient;
import org.springframework.stereotype.Service;

import java.util.Set;
import java.util.List;
import java.util.Optional;
import java.time.LocalDate;
import java.util.ArrayList;


@Service
public class MarketDataServiceImpl implements MarketDataService {

    private static final Set<String> SUPPORTED_INTERVALS = Set.of(
        "1min", "5min", "15min", "30min", "45min",
        "1h", "2h", "4h", "8h",
        "1day", "1week", "1month"
    );

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
    public List<MarketDataHistoryResponseDTO> fetchHistoricalData(String symbol, LocalDate startDate, LocalDate endDate, String interval) {

        if (interval == null || !SUPPORTED_INTERVALS.contains(interval)) {
            throw new IllegalArgumentException(
                "Unsupported interval: " + interval
            );
        }
        
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
            Optional<MarketData> existingRecord = 
                marketDataRepository.findByAssetAndTimestamp(
                    asset, dto.getTimestamp()
                );

            MarketData marketData;

            if (existingRecord.isPresent()) {
                marketData = existingRecord.get();

                marketData.updateMarketValues(
                    dto.getOpen(), 
                    dto.getHigh(), 
                    dto.getLow(), 
                    dto.getClose(), 
                    dto.getVolume()
                );
            } else {
                marketData = new MarketData(
                    dto.getOpen(),
                    dto.getHigh(),
                    dto.getLow(),
                    dto.getClose(),
                    dto.getVolume(),
                    asset,
                    dto.getTimestamp()
                );
            }
            
            marketDataList.add(marketData);
        }

        marketDataRepository.saveAll(marketDataList);

        List<MarketDataHistoryResponseDTO> responseList = new ArrayList<>();
        
        for (MarketData marketData : marketDataList) {
        
            MarketDataHistoryResponseDTO response =
                new MarketDataHistoryResponseDTO(
                    symbol,
                    marketData.getOpen(),
                    marketData.getHigh(),
                    marketData.getLow(),
                    marketData.getClose(),
                    marketData.getVolume(),
                    marketData.getTimestamp()
                );
        
            responseList.add(response);
        }

        return responseList;
    }
}