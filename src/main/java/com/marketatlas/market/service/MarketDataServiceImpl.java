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
import java.util.HashSet;
import java.util.Optional;
import java.time.LocalDate;
import java.util.ArrayList;

import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;


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

        if (startDate != null && endDate != null && startDate.isAfter(endDate)) {
            throw new IllegalArgumentException(
                    "startDate must be before or equal to endDate"
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

        Set<LocalDate> timestamps = new HashSet<>();

        for (MarketDataDTO dto : marketDataDTOs) {
            if (!timestamps.add(dto.getTimestamp())) {
                throw new IllegalArgumentException(
                        "Duplicate market data timestamp: " + dto.getTimestamp()
                );
            }
        }

        List<MarketData> marketDataList = new ArrayList<>();

        if (marketDataDTOs.isEmpty()) {
            return new ArrayList<>();
        }

        List<LocalDate> requestedTimestamps = marketDataDTOs.stream()
                .map(MarketDataDTO::getTimestamp)
                .toList();

        Map<LocalDate, MarketData> existingRecords =
                marketDataRepository.findByAssetAndTimestampIn(
                        asset,
                        requestedTimestamps
                ).stream().collect(Collectors.toMap(
                        MarketData::getTimestamp,
                        Function.identity()
                ));


        for (MarketDataDTO dto : marketDataDTOs) {
            MarketData marketData = existingRecords.get(dto.getTimestamp());

            if (marketData != null) {
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