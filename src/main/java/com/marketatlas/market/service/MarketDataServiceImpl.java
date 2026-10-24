package com.marketatlas.market.service;

import com.marketatlas.common.exception.MarketDataProviderException;
import com.marketatlas.common.exception.ResourceNotFoundException;
import com.marketatlas.market.client.MarketDataClient;
import com.marketatlas.market.dto.MarketDataDTO;
import com.marketatlas.market.dto.MarketDataHistoryResponseDTO;
import com.marketatlas.market.entity.Asset;
import com.marketatlas.market.entity.MarketData;
import com.marketatlas.market.repository.MarketDataRepository;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.LocalDate;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;

@Service
public class MarketDataServiceImpl implements MarketDataService {

    private static final String SUPPORTED_INTERVAL = "1day";
    private static final int MAX_RANGE_YEARS = 10;

    private final MarketDataRepository marketDataRepository;
    private final MarketDataCoverageService coverageService;
    private final AssetService assetService;
    private final MarketDataClient marketDataClient;
    private final Clock clock;

    public MarketDataServiceImpl(
            MarketDataRepository marketDataRepository,
            AssetService assetService,
            MarketDataClient marketDataClient,
            MarketDataCoverageService coverageService,
            Clock clock) {
        this.marketDataRepository = marketDataRepository;
        this.assetService = assetService;
        this.marketDataClient = marketDataClient;
        this.coverageService = coverageService;
        this.clock = clock;
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
    public List<MarketDataHistoryResponseDTO> fetchHistoricalData(
            String symbol,
            LocalDate startDate,
            LocalDate endDate,
            String interval) {

        if (!SUPPORTED_INTERVAL.equals(interval)) {
            throw new IllegalArgumentException(
                    "Unsupported interval: " + interval
                            + ". Only " + SUPPORTED_INTERVAL + " is currently supported."
            );
        }

        if (startDate == null || endDate == null) {
            throw new IllegalArgumentException(
                    "startDate and endDate are required"
            );
        }

        if (startDate.isAfter(endDate)) {
            throw new IllegalArgumentException(
                    "startDate must be before or equal to endDate"
            );
        }

        if (startDate.plusYears(MAX_RANGE_YEARS).isBefore(endDate)) {
            throw new IllegalArgumentException(
                    "The requested range must not exceed "
                            + MAX_RANGE_YEARS + " years"
            );
        }

        if (symbol == null || symbol.isBlank()) {
            throw new IllegalArgumentException("symbol must not be blank");
        }

        String normalizedSymbol = symbol.trim().toUpperCase(Locale.ROOT);

        Asset asset = assetService.findAssetBySymbol(normalizedSymbol)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Asset not found: " + normalizedSymbol
                ));

        LocalDate today = LocalDate.now(clock);
        LocalDate effectiveEnd = endDate.isAfter(today) ? today : endDate;

        if (startDate.isAfter(effectiveEnd)) {
            return List.of();
        }

        List<DateRange> missing = coverageService.findMissingRanges(
                asset.getId(), new DateRange(startDate, effectiveEnd));

        if (!missing.isEmpty()) {
            // One provider call per request: span from the first gap to the
            // last. Re-fetching a few covered days is harmless (idempotent
            // upsert) and keeps us inside the provider's rate limits.
            DateRange toFetch = new DateRange(
                    missing.getFirst().start(), missing.getLast().end());

            List<MarketDataDTO> fetched = marketDataClient.getHistoricalData(
                    normalizedSymbol, toFetch.start(), toFetch.end(), interval);

            rejectDuplicateTimestamps(fetched);

            coverageService.saveFetchedRange(asset.getId(), toFetch, fetched);
        }

        return marketDataRepository
                .findByAssetAndTimestampBetweenOrderByTimestampAsc(
                        asset, startDate, effectiveEnd)
                .stream()
                .map(row -> new MarketDataHistoryResponseDTO(
                        normalizedSymbol,
                        row.getOpen(),
                        row.getHigh(),
                        row.getLow(),
                        row.getClose(),
                        row.getVolume(),
                        row.getTimestamp()))
                .toList();
    }

    private void rejectDuplicateTimestamps(List<MarketDataDTO> data) {
        Set<LocalDate> seen = new HashSet<>();
        for (MarketDataDTO dto : data) {
            if (!seen.add(dto.getTimestamp())) {
                throw new MarketDataProviderException(
                        "Market data provider returned a duplicate timestamp: "
                                + dto.getTimestamp()
                );
            }
        }
    }
}