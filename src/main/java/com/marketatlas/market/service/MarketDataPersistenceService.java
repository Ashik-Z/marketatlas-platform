package com.marketatlas.market.service;

import com.marketatlas.market.dto.MarketDataDTO;
import com.marketatlas.market.entity.Asset;
import com.marketatlas.market.repository.MarketDataRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

@Service
public class MarketDataPersistenceService {

    private final MarketDataRepository marketDataRepository;

    public MarketDataPersistenceService(MarketDataRepository marketDataRepository) {
        this.marketDataRepository = marketDataRepository;
    }

    /**
     * Writes all bars in a single statement and returns the number of rows written.
     * Bars are de-duplicated by date (last one wins) and written in timestamp order.
     */
    @Transactional
    public int persistHistoricalData(Asset asset, List<MarketDataDTO> marketDataDTOs) {
        if (marketDataDTOs.isEmpty()) {
            return 0;
        }

        Map<LocalDate, MarketDataDTO> byDate = new TreeMap<>();
        for (MarketDataDTO dto : marketDataDTOs) {
            byDate.put(dto.getTimestamp(), dto);
        }

        int n = byDate.size();
        LocalDate[] timestamps = new LocalDate[n];
        Double[] opens = new Double[n];
        Double[] highs = new Double[n];
        Double[] lows = new Double[n];
        Double[] closes = new Double[n];
        Double[] volumes = new Double[n];

        int i = 0;
        for (MarketDataDTO dto : byDate.values()) {
            timestamps[i] = dto.getTimestamp();
            opens[i] = dto.getOpen();
            highs[i] = dto.getHigh();
            lows[i] = dto.getLow();
            closes[i] = dto.getClose();
            volumes[i] = dto.getVolume();
            i++;
        }

        return marketDataRepository.upsertBatch(
                asset.getId(), timestamps, opens, highs, lows, closes, volumes);
    }
}