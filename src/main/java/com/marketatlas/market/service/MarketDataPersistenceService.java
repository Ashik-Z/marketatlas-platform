
package com.marketatlas.market.service;

import com.marketatlas.market.dto.MarketDataDTO;
import com.marketatlas.market.entity.Asset;
import com.marketatlas.market.entity.MarketData;
import com.marketatlas.market.repository.MarketDataRepository;
import jakarta.persistence.EntityManager;
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
    private final EntityManager entityManager;

    public MarketDataPersistenceService(
            MarketDataRepository marketDataRepository,
            EntityManager entityManager) {
        this.marketDataRepository = marketDataRepository;
        this.entityManager = entityManager;
    }

    @Transactional
    public List<MarketData> persistHistoricalData(
            Asset asset,
            List<MarketDataDTO> marketDataDTOs) {

        if (marketDataDTOs.isEmpty()) {
            return new ArrayList<>();
        }

        entityManager.flush();

        for (MarketDataDTO dto : marketDataDTOs) {
            marketDataRepository.upsertMarketData(
                    asset.getId(),
                    dto.getTimestamp(),
                    dto.getOpen(),
                    dto.getHigh(),
                    dto.getLow(),
                    dto.getClose(),
                    dto.getVolume()
            );
        }

        List<LocalDate> requestedTimestamps = marketDataDTOs.stream()
                .map(MarketDataDTO::getTimestamp)
                .toList();

        List<MarketData> records = marketDataRepository
                .findByAssetAndTimestampIn(asset, requestedTimestamps);

        for (MarketData record : records) {
            entityManager.refresh(record);
        }

        Map<LocalDate, MarketData> persistedRecords = records.stream()
                .collect(Collectors.toMap(
                        MarketData::getTimestamp,
                        Function.identity()
                ));

        return marketDataDTOs.stream()
                .map(dto -> persistedRecords.get(dto.getTimestamp()))
                .toList();
    }
}
