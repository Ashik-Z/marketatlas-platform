package com.marketatlas.market.service;

import com.marketatlas.market.entity.Asset;
import com.marketatlas.market.entity.Issuer;
import com.marketatlas.market.entity.MarketData;
import com.marketatlas.market.client.MarketDataClient;
import com.marketatlas.market.repository.MarketDataRepository;
import com.marketatlas.enums.AssetClass;
import com.marketatlas.market.dto.MarketDataDTO;
import com.marketatlas.market.dto.MarketDataHistoryResponseDTO;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import org.mockito.Mock;
import static org.mockito.Mockito.never;
import org.mockito.junit.jupiter.MockitoExtension;
import static org.mockito.ArgumentMatchers.anyList;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.verifyNoInteractions;

import java.util.List;
import java.util.Optional;
import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.marketatlas.common.exception.MarketDataProviderException;
import com.marketatlas.common.exception.ResourceNotFoundException;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;

import static org.mockito.ArgumentMatchers.any;

@ExtendWith(MockitoExtension.class)
class MarketDataServiceTest {

    @Mock
    private MarketDataRepository marketDataRepository;

    @Mock
    private MarketDataCoverageService coverageService;

    @Mock
    private AssetService assetService;

    @Mock
    private MarketDataClient marketDataClient;

    private MarketDataServiceImpl marketDataService;

    private static final LocalDate TODAY = LocalDate.of(2026, 10, 24);

    private Asset apple;

    @BeforeEach
    void setUp() {
        Clock clock = Clock.fixed(
                Instant.parse("2026-10-24T12:00:00Z"), ZoneOffset.UTC);

        marketDataService = new MarketDataServiceImpl(
                marketDataRepository, assetService, marketDataClient,
                coverageService, clock);

        apple = new Asset("Apple", AssetClass.EQUITY,
                new Issuer("Apple Inc.", "United States"));
        ReflectionTestUtils.setField(apple, "id", 7L);
    }

    private static DateRange range(LocalDate from, LocalDate to) {
        return new DateRange(from, to);
    }

    private static LocalDate oct(int day) {
        return LocalDate.of(2026, 10, day);
    }

    private MarketData row(double close, LocalDate date) {
        return new MarketData(close - 1, close + 1, close - 2, close,
                1000.0, apple, date);
    }

    private MarketDataDTO dto(double close, LocalDate date) {
        return new MarketDataDTO(close - 1, close + 1, close - 2, close,
                1000.0, date);
    }

    private void stubApple() {
        when(assetService.findAssetBySymbol("AAPL"))
                .thenReturn(Optional.of(apple));
    }

    @Test
    void shouldCreateMarketData() {

        Issuer appleIssuer = new Issuer(
            "Apple Inc.",
            "United States"
        );

        Asset apple = new Asset(
            "Apple",
            AssetClass.EQUITY,
            appleIssuer
        );

        MarketData marketData = new MarketData(
            254.80,
            255.30,
            254.60,
            255.10,
            183421.0,
            apple,
            LocalDate.of(2026, 10, 1)
        );

        when(marketDataRepository.save(marketData))
            .thenReturn(marketData);

        MarketData savedMarketData =
            marketDataService.createMarketData(marketData);

        assertEquals(marketData, savedMarketData);

        verify(marketDataRepository)
            .save(marketData);
    }

    @Test
    void shouldFindMarketDataByAsset() {

        Issuer appleIssuer = new Issuer(
            "Apple Inc.",
            "United States"
        );

        Asset apple = new Asset(
            "Apple",
            AssetClass.EQUITY,
            appleIssuer
        );

        MarketData firstObservation = new MarketData(
            254.80,
            255.30,
            254.60,
            255.10,
            183421.0,
            apple,
            LocalDate.of(2026, 10, 1)
        );

        MarketData secondObservation = new MarketData(
            255.10,
            255.50,
            254.90,
            255.42,
            142310.0,
            apple,
            LocalDate.of(2026, 10, 1)
        );

        List<MarketData> marketData =
            List.of(firstObservation, secondObservation);

        when(marketDataRepository.findByAsset(apple))
            .thenReturn(marketData);

        List<MarketData> result =
            marketDataService.findMarketDataByAsset(apple);

        assertEquals(2, result.size());
        assertEquals(firstObservation, result.get(0));
        assertEquals(secondObservation, result.get(1));

        verify(marketDataRepository)
            .findByAsset(apple);
    }

    @Test
    void shouldFindMarketDataById() {

        Issuer appleIssuer = new Issuer(
            "Apple Inc.",
            "United States"
        );

        Asset apple = new Asset(
            "Apple",
            AssetClass.EQUITY,
            appleIssuer
        );

        MarketData marketData = new MarketData(
            254.80,
            255.30,
            254.60,
            255.10,
            183421.0,
            apple,
            LocalDate.of(2026, 10, 1)
        );

        Long id = 1L;

        when(marketDataRepository.findById(id))
            .thenReturn(Optional.of(marketData));

        Optional<MarketData> result =
            marketDataService.findMarketDataById(id);

        assertTrue(result.isPresent());
        assertEquals(marketData, result.get());

        verify(marketDataRepository)
            .findById(id);
    }

    @Test
    void shouldFetchMissingRangeThenServeFromDatabase() {
        stubApple();
        List<MarketDataDTO> fetched = List.of(dto(101, oct(1)), dto(102, oct(2)));

        when(coverageService.findMissingRanges(7L, range(oct(1), oct(2))))
                .thenReturn(List.of(range(oct(1), oct(2))));
        when(marketDataClient.getHistoricalData("AAPL", oct(1), oct(2), "1day"))
                .thenReturn(fetched);
        when(marketDataRepository.findByAssetAndTimestampBetweenOrderByTimestampAsc(
                apple, oct(1), oct(2)))
                .thenReturn(List.of(row(101, oct(1)), row(102, oct(2))));

        List<MarketDataHistoryResponseDTO> result =
                marketDataService.fetchHistoricalData("AAPL", oct(1), oct(2), "1day");

        assertEquals(2, result.size());
        assertEquals("AAPL", result.get(0).getSymbol());
        assertEquals(101.0, result.get(0).getClose());
        assertEquals(oct(1), result.get(0).getTimestamp());
        assertEquals(oct(2), result.get(1).getTimestamp());

        verify(coverageService).saveFetchedRange(7L, range(oct(1), oct(2)), fetched);
    }

    @Test
    void shouldNormalizeSymbolBeforeLookupAndProviderCall() {
        stubApple();
        when(coverageService.findMissingRanges(7L, range(oct(1), oct(1))))
                .thenReturn(List.of(range(oct(1), oct(1))));
        when(marketDataClient.getHistoricalData("AAPL", oct(1), oct(1), "1day"))
                .thenReturn(List.of());

        marketDataService.fetchHistoricalData(" aapl ", oct(1), oct(1), "1day");

        verify(assetService).findAssetBySymbol("AAPL");
        verify(marketDataClient).getHistoricalData("AAPL", oct(1), oct(1), "1day");
    }

    @Test
    void shouldNotCallProviderWhenRangeIsFullyCovered() {
        stubApple();
        when(coverageService.findMissingRanges(7L, range(oct(1), oct(2))))
                .thenReturn(List.of());
        when(marketDataRepository.findByAssetAndTimestampBetweenOrderByTimestampAsc(
                apple, oct(1), oct(2)))
                .thenReturn(List.of(row(101, oct(1))));

        List<MarketDataHistoryResponseDTO> result =
                marketDataService.fetchHistoricalData("AAPL", oct(1), oct(2), "1day");

        assertEquals(1, result.size());
        verifyNoInteractions(marketDataClient);
        verify(coverageService, never()).saveFetchedRange(any(), any(), any());
    }

    @Test
    void shouldMakeOneProviderCallSpanningAllGaps() {
        stubApple();
        List<MarketDataDTO> fetched = List.of(dto(101, oct(1)));

        when(coverageService.findMissingRanges(7L, range(oct(1), oct(10))))
                .thenReturn(List.of(range(oct(1), oct(3)), range(oct(8), oct(10))));
        when(marketDataClient.getHistoricalData("AAPL", oct(1), oct(10), "1day"))
                .thenReturn(fetched);

        marketDataService.fetchHistoricalData("AAPL", oct(1), oct(10), "1day");

        verify(marketDataClient).getHistoricalData("AAPL", oct(1), oct(10), "1day");
        verify(coverageService).saveFetchedRange(7L, range(oct(1), oct(10)), fetched);
    }

    @Test
    void shouldRecordCoverageEvenWhenProviderReturnsNoRows() {
        stubApple();
        when(coverageService.findMissingRanges(7L, range(oct(17), oct(18))))
                .thenReturn(List.of(range(oct(17), oct(18))));
        when(marketDataClient.getHistoricalData("AAPL", oct(17), oct(18), "1day"))
                .thenReturn(List.of());

        List<MarketDataHistoryResponseDTO> result =
                marketDataService.fetchHistoricalData("AAPL", oct(17), oct(18), "1day");

        assertTrue(result.isEmpty());
        verify(coverageService).saveFetchedRange(7L, range(oct(17), oct(18)), List.of());
    }

    @Test
    void shouldClampFutureEndDateToToday() {
        stubApple();
        when(coverageService.findMissingRanges(7L, range(oct(20), TODAY)))
                .thenReturn(List.of());

        marketDataService.fetchHistoricalData(
                "AAPL", oct(20), LocalDate.of(2026, 12, 31), "1day");

        verify(coverageService).findMissingRanges(7L, range(oct(20), TODAY));
        verify(marketDataRepository)
                .findByAssetAndTimestampBetweenOrderByTimestampAsc(apple, oct(20), TODAY);
    }

    @Test
    void shouldReturnEmptyWhenWholeRangeIsInTheFuture() {
        stubApple();

        List<MarketDataHistoryResponseDTO> result =
                marketDataService.fetchHistoricalData(
                        "AAPL", LocalDate.of(2026, 11, 1),
                        LocalDate.of(2026, 11, 5), "1day");

        assertTrue(result.isEmpty());
        verifyNoInteractions(coverageService, marketDataClient, marketDataRepository);
    }

    @Test
    void shouldReturnNotFoundForUnknownSymbol() {
        when(assetService.findAssetBySymbol("NOPE")).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class,
                () -> marketDataService.fetchHistoricalData("nope", oct(1), oct(2), "1day"));

        verifyNoInteractions(coverageService, marketDataClient, marketDataRepository);
    }

    @Test
    void shouldRejectInvalidDateRangeBeforeCallingDependencies() {
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> marketDataService.fetchHistoricalData("AAPL", oct(5), oct(1), "1day"));

        assertEquals("startDate must be before or equal to endDate", exception.getMessage());
        verifyNoInteractions(assetService, marketDataClient, marketDataRepository, coverageService);
    }

    @Test
    void shouldRejectNonDailyIntervalsBeforeCallingDependencies() {
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> marketDataService.fetchHistoricalData("AAPL", oct(1), oct(5), "1week"));

        assertEquals("Unsupported interval: 1week. Only 1day is currently supported.",
                exception.getMessage());
        verifyNoInteractions(assetService, marketDataClient, marketDataRepository, coverageService);
    }

    @Test
    void shouldRejectRangesLongerThanTenYears() {
        assertThrows(IllegalArgumentException.class,
                () -> marketDataService.fetchHistoricalData(
                        "AAPL", LocalDate.of(2015, 1, 1), LocalDate.of(2026, 1, 1), "1day"));

        verifyNoInteractions(assetService, marketDataClient, marketDataRepository, coverageService);
    }

    @Test
    void shouldRejectBlankSymbol() {
        assertThrows(IllegalArgumentException.class,
                () -> marketDataService.fetchHistoricalData("  ", oct(1), oct(2), "1day"));

        verifyNoInteractions(assetService, marketDataClient, marketDataRepository, coverageService);
    }

    @Test
    void shouldTreatDuplicateProviderTimestampsAsProviderFault() {
        stubApple();
        when(coverageService.findMissingRanges(7L, range(oct(1), oct(1))))
                .thenReturn(List.of(range(oct(1), oct(1))));
        when(marketDataClient.getHistoricalData("AAPL", oct(1), oct(1), "1day"))
                .thenReturn(List.of(dto(101, oct(1)), dto(102, oct(1))));

        assertThrows(MarketDataProviderException.class,
                () -> marketDataService.fetchHistoricalData("AAPL", oct(1), oct(1), "1day"));

        verify(coverageService, never()).saveFetchedRange(any(), any(), any());
    }
}
