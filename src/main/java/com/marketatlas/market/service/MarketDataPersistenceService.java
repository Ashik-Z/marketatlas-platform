package com.marketatlas.market.service;

import com.marketatlas.market.dto.MarketDataDTO;
import com.marketatlas.market.entity.Asset;
import com.marketatlas.market.entity.MarketData;
import com.marketatlas.market.repository.MarketDataRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class MarketDataPersistenceService {

    private final MarketDataRepository marketDataRepository;

    public MarketDataPersistenceService(
            MarketDataRepository marketDataRepository) {
        this.marketDataRepository = marketDataRepository;
    }

    @Transactional
    public List<MarketData> persistHistoricalData(
            Asset asset,
            List<MarketDataDTO> marketDataDTOs) {

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

        List<MarketData> marketDataList = new ArrayList<>();

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

        return marketDataRepository.saveAll(marketDataList);
    }
}
