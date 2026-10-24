package com.marketatlas.market.service;

import com.marketatlas.common.exception.ResourceNotFoundException;
import com.marketatlas.market.dto.MarketDataDTO;
import com.marketatlas.market.entity.Asset;
import com.marketatlas.market.entity.MarketDataCoverage;
import com.marketatlas.market.repository.AssetRepository;
import com.marketatlas.market.repository.MarketDataCoverageRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDate;
import java.util.List;

@Service
public class MarketDataCoverageService {

    /** A day's bar is treated as final once it is at least this many days old. */
    static final int FINALIZATION_DELAY_DAYS = 2;

    private final AssetRepository assetRepository;
    private final MarketDataCoverageRepository coverageRepository;
    private final MarketDataPersistenceService persistenceService;
    private final Clock clock;

    public MarketDataCoverageService(
            AssetRepository assetRepository,
            MarketDataCoverageRepository coverageRepository,
            MarketDataPersistenceService persistenceService,
            Clock clock) {
        this.assetRepository = assetRepository;
        this.coverageRepository = coverageRepository;
        this.persistenceService = persistenceService;
        this.clock = clock;
    }

    @Transactional(readOnly = true)
    public List<DateRange> findMissingRanges(Long assetId, DateRange requested) {
        List<DateRange> covered = coverageRepository
                .findOverlapping(assetId, requested.start(), requested.end())
                .stream()
                .map(c -> new DateRange(c.getStartDate(), c.getEndDate()))
                .toList();

        return DateRange.missing(requested, covered);
    }

    /**
     * Stores freshly fetched bars and records which dates are now covered, in
     * one transaction, so coverage can never exist without its data. The
     * asset row is locked so concurrent merges for the same asset queue up.
     */
    @Transactional
    public void saveFetchedRange(Long assetId, DateRange fetched, List<MarketDataDTO> data) {
        Asset asset = assetRepository.findByIdForUpdate(assetId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Asset " + assetId + " was not found."));

        if (!data.isEmpty()) {
            persistenceService.persistHistoricalData(asset, data);
        }

        LocalDate lastFinalDay = LocalDate.now(clock).minusDays(FINALIZATION_DELAY_DAYS);
        if (fetched.start().isAfter(lastFinalDay)) {
            return;
        }

        LocalDate start = fetched.start();
        LocalDate end = fetched.end().isAfter(lastFinalDay) ? lastFinalDay : fetched.end();

        List<MarketDataCoverage> touching = coverageRepository.findOverlapping(
                assetId, start.minusDays(1), end.plusDays(1));

        for (MarketDataCoverage existing : touching) {
            if (existing.getStartDate().isBefore(start)) {
                start = existing.getStartDate();
            }
            if (existing.getEndDate().isAfter(end)) {
                end = existing.getEndDate();
            }
        }

        coverageRepository.deleteAll(touching);
        coverageRepository.save(new MarketDataCoverage(asset, start, end));
    }
}