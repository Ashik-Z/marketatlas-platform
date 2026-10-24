package com.marketatlas.market.service;

import com.marketatlas.common.exception.ResourceNotFoundException;
import com.marketatlas.enums.AssetClass;
import com.marketatlas.market.TestPostgresConfiguration;
import com.marketatlas.market.dto.MarketDataDTO;
import com.marketatlas.market.entity.Asset;
import com.marketatlas.market.entity.Issuer;
import com.marketatlas.market.entity.MarketDataCoverage;
import com.marketatlas.market.repository.AssetRepository;
import com.marketatlas.market.repository.IssuerRepository;
import com.marketatlas.market.repository.MarketDataCoverageRepository;
import com.marketatlas.market.repository.MarketDataRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
@Import(TestPostgresConfiguration.class)
@Transactional
class MarketDataCoverageServiceTest {

    @TestConfiguration
    static class FixedClockConfig {
        @Bean
        @Primary
        Clock fixedClock() {
            // "today" is 2026-10-24, so the last final day is 2026-10-22
            return Clock.fixed(Instant.parse("2026-10-24T12:00:00Z"), ZoneOffset.UTC);
        }
    }

    @Autowired
    private MarketDataCoverageService coverageService;

    @Autowired
    private MarketDataCoverageRepository coverageRepository;

    @Autowired
    private MarketDataRepository marketDataRepository;

    @Autowired
    private AssetRepository assetRepository;

    @Autowired
    private IssuerRepository issuerRepository;

    private Long assetId;

    @BeforeEach
    void setUp() {
        Issuer issuer = issuerRepository.save(new Issuer("Apple Inc.", "United States"));
        assetId = assetRepository.save(new Asset("Apple", AssetClass.EQUITY, issuer)).getId();
    }

    private static LocalDate oct(int day) {
        return LocalDate.of(2026, 10, day);
    }

    private static DateRange range(int from, int to) {
        return new DateRange(oct(from), oct(to));
    }

    private static MarketDataDTO dto(int day) {
        return new MarketDataDTO(100.0, 102.0, 99.0, 101.0, 1000.0, oct(day));
    }

    private List<MarketDataCoverage> storedCoverage() {
        return coverageRepository.findOverlapping(
                assetId, LocalDate.of(2000, 1, 1), LocalDate.of(2100, 1, 1));
    }

    @Test
    void shouldStoreDataAndCoverageTogether() {
        coverageService.saveFetchedRange(assetId, range(1, 10), List.of(dto(1), dto(2)));

        assertEquals(2, marketDataRepository.count());
        assertTrue(coverageService.findMissingRanges(assetId, range(1, 10)).isEmpty());
    }

    @Test
    void shouldReportOnlyTheUncoveredPart() {
        coverageService.saveFetchedRange(assetId, range(1, 5), List.of());

        assertEquals(
                List.of(range(6, 10)),
                coverageService.findMissingRanges(assetId, range(1, 10)));
    }

    @Test
    void shouldMergeAdjacentAndOverlappingRanges() {
        coverageService.saveFetchedRange(assetId, range(1, 5), List.of());
        coverageService.saveFetchedRange(assetId, range(6, 10), List.of());

        List<MarketDataCoverage> afterAdjacent = storedCoverage();
        assertEquals(1, afterAdjacent.size());
        assertEquals(oct(1), afterAdjacent.get(0).getStartDate());
        assertEquals(oct(10), afterAdjacent.get(0).getEndDate());

        coverageService.saveFetchedRange(assetId, range(8, 15), List.of());

        List<MarketDataCoverage> afterOverlap = storedCoverage();
        assertEquals(1, afterOverlap.size());
        assertEquals(oct(1), afterOverlap.get(0).getStartDate());
        assertEquals(oct(15), afterOverlap.get(0).getEndDate());
    }

    @Test
    void shouldKeepSeparateRangesWhenThereIsAGap() {
        coverageService.saveFetchedRange(assetId, range(1, 5), List.of());
        coverageService.saveFetchedRange(assetId, range(8, 10), List.of());

        assertEquals(2, storedCoverage().size());
        assertEquals(
                List.of(range(6, 7)),
                coverageService.findMissingRanges(assetId, range(1, 10)));
    }

    @Test
    void shouldNotMarkRecentDaysAsCovered() {
        coverageService.saveFetchedRange(assetId, range(20, 24), List.of(dto(23), dto(24)));

        // data is stored, but only up to the last final day (10-22) counts as covered
        assertEquals(2, marketDataRepository.count());
        List<MarketDataCoverage> stored = storedCoverage();
        assertEquals(1, stored.size());
        assertEquals(oct(22), stored.get(0).getEndDate());
        assertEquals(
                List.of(range(23, 24)),
                coverageService.findMissingRanges(assetId, range(20, 24)));
    }

    @Test
    void shouldRecordNoCoverageWhenWholeRangeIsRecent() {
        coverageService.saveFetchedRange(assetId, range(23, 24), List.of(dto(23)));

        assertEquals(1, marketDataRepository.count());
        assertTrue(storedCoverage().isEmpty());
    }

    @Test
    void shouldRecordCoverageEvenWhenProviderReturnedNoRows() {
        coverageService.saveFetchedRange(assetId, range(17, 18), List.of());

        assertEquals(0, marketDataRepository.count());
        assertTrue(coverageService.findMissingRanges(assetId, range(17, 18)).isEmpty());
    }

    @Test
    void shouldRejectUnknownAsset() {
        assertThrows(ResourceNotFoundException.class,
                () -> coverageService.saveFetchedRange(999999L, range(1, 5), List.of()));
    }
}