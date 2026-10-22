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

@ExtendWith(MockitoExtension.class)
class MarketDataServiceTest {

    @Mock
    private MarketDataRepository marketDataRepository;

    @Mock
    private AssetService assetService;
    
    @Mock
    private MarketDataClient marketDataClient;

    private MarketDataServiceImpl marketDataService;

    @BeforeEach
    void setUp() {
        marketDataService =
            new MarketDataServiceImpl(marketDataRepository, assetService, marketDataClient);
    }

    @Test
    void shouldCreateMarketData() {

        Issuer appleIssuer = new Issuer(
            "Apple Inc.",
            "United States"
        );

        Asset apple = new Asset(
            "Apple",
            "AAPL",
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
            "AAPL",
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
            "AAPL",
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
    void shouldFetchHistoricalData() {
    
        Issuer appleIssuer = new Issuer(
            "Apple Inc.",
            "United States"
        );
    
        Asset apple = new Asset(
            "Apple",
            "AAPL",
            AssetClass.EQUITY,
            appleIssuer
        );
    
        LocalDate startDate = LocalDate.of(2026, 9, 29);
        LocalDate endDate = LocalDate.of(2026, 10, 2);
    
        MarketDataDTO firstDTO = new MarketDataDTO(
            254.80,
            255.30,
            254.60,
            255.10,
            183421.0,
            LocalDate.of(2026, 10, 1)
        );
    
        MarketDataDTO secondDTO = new MarketDataDTO(
            255.10,
            255.50,
            254.90,
            255.42,
            142310.0,
            LocalDate.of(2026, 10, 2)
        );
    
        List<MarketDataDTO> marketDataDTOs =
            List.of(firstDTO, secondDTO);
    
        when(assetService.findAssetBySymbol("AAPL"))
            .thenReturn(Optional.of(apple));
    
        when(marketDataClient.getHistoricalData(
            "AAPL",
            startDate,
            endDate,
            "1day"
        )).thenReturn(marketDataDTOs);
    
        List<MarketDataHistoryResponseDTO> result =
            marketDataService.fetchHistoricalData(
                "AAPL",
                startDate,
                endDate,
                "1day"
            );
    
        assertEquals(2, result.size());
    
        assertEquals("AAPL", result.get(0).getSymbol());
        assertEquals(254.80, result.get(0).getOpen());
        assertEquals(255.30, result.get(0).getHigh());
        assertEquals(254.60, result.get(0).getLow());
        assertEquals(255.10, result.get(0).getClose());
        assertEquals(183421.0, result.get(0).getVolume());
        assertEquals(
            LocalDate.of(2026, 10, 1),
            result.get(0).getTimestamp()
        );
    
        assertEquals("AAPL", result.get(1).getSymbol());
        assertEquals(
            LocalDate.of(2026, 10, 2),
            result.get(1).getTimestamp()
        );
    
        verify(assetService)
            .findAssetBySymbol("AAPL");
    
        verify(marketDataClient)
            .getHistoricalData(
                "AAPL",
                startDate,
                endDate,
                "1day"
            );
    
        verify(marketDataRepository)
            .saveAll(org.mockito.ArgumentMatchers.anyList());
    }

    @Test
    void shouldReturnEmptyListWhenProviderReturnsNoHistoricalData() {
        Issuer appleIssuer = new Issuer("Apple Inc.", "United States");
        Asset apple = new Asset("Apple", "AAPL", AssetClass.EQUITY, appleIssuer);

        LocalDate date = LocalDate.of(2026, 10, 1);

        when(assetService.findAssetBySymbol("AAPL"))
                .thenReturn(Optional.of(apple));

        when(marketDataClient.getHistoricalData(
                "AAPL", date, date, "1day"
        )).thenReturn(List.of());

        List<MarketDataHistoryResponseDTO> result =
                marketDataService.fetchHistoricalData(
                        "AAPL", date, date, "1day"
                );

        assertTrue(result.isEmpty());

        verify(marketDataRepository, never())
                .findByAssetAndTimestampIn(
                        org.mockito.ArgumentMatchers.eq(apple),
                        org.mockito.ArgumentMatchers.anyCollection()
                );

        verify(marketDataRepository, never())
                .saveAll(anyList());
    }
    
    @Test
    void shouldUpdateExistingMarketDataInsteadOfCreatingDuplicate() {
        Issuer appleIssuer = new Issuer(
            "Apple Inc.",
            "United States"
        );
    
        Asset apple = new Asset(
            "Apple",
            "AAPL",
            AssetClass.EQUITY,
            appleIssuer
        );
    
        LocalDate date = LocalDate.of(2026, 10, 1);
    
        MarketData existingRecord = new MarketData(
            250.00,
            252.00,
            249.00,
            251.00,
            100000.0,
            apple,
            date
        );
    
        MarketDataDTO updatedDTO = new MarketDataDTO(
            254.80,
            255.30,
            254.60,
            255.10,
            183421.0,
            date
        );
    
        when(assetService.findAssetBySymbol("AAPL"))
            .thenReturn(Optional.of(apple));
    
        when(marketDataClient.getHistoricalData(
            "AAPL",
            date,
            date,
            "1day"
        )).thenReturn(List.of(updatedDTO));

        when(marketDataRepository.findByAssetAndTimestampIn(
                apple,
                List.of(date)
        )).thenReturn(List.of(existingRecord));
    
        List<MarketDataHistoryResponseDTO> result =
            marketDataService.fetchHistoricalData(
                "AAPL",
                date,
                date,
                "1day"
            );

        assertEquals(254.80, existingRecord.getOpen());
        assertEquals(255.30, existingRecord.getHigh());
        assertEquals(254.60, existingRecord.getLow());
        assertEquals(255.10, existingRecord.getClose());
        assertEquals(183421.0, existingRecord.getVolume());

        assertEquals(1, result.size());
        assertEquals(255.10, result.get(0).getClose());

        verify(marketDataRepository)
                .findByAssetAndTimestampIn(apple, List.of(date));

        org.mockito.ArgumentCaptor<List<MarketData>> captor =
            org.mockito.ArgumentCaptor.forClass(List.class);
    
        verify(marketDataRepository).saveAll(captor.capture());
    
        List<MarketData> savedRecords = captor.getValue();
    
        assertEquals(1, savedRecords.size());
        assertTrue(savedRecords.get(0) == existingRecord);
    }

    @Test
    void shouldRejectInvalidDateRangeBeforeCallingDependencies() {
        LocalDate startDate = LocalDate.of(2026, 10, 5);
        LocalDate endDate = LocalDate.of(2026, 10, 1);

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> marketDataService.fetchHistoricalData(
                        "AAPL",
                        startDate,
                        endDate,
                        "1day"
                )
        );

        assertEquals(
                "startDate must be before or equal to endDate",
                exception.getMessage()
        );

        verifyNoInteractions(
                assetService,
                marketDataClient,
                marketDataRepository
        );
    }

    @Test
    void shouldRejectUnsupportedIntervalBeforeCallingDependencies() {
        IllegalArgumentException exception = assertThrows(
            IllegalArgumentException.class,
            () -> marketDataService.fetchHistoricalData(
                "AAPL",
                LocalDate.of(2026, 10, 1),
                LocalDate.of(2026, 10, 5),
                "abc"
            )
        );
    
        assertEquals("Unsupported interval: abc", exception.getMessage());
    
        verifyNoInteractions(
            assetService,
            marketDataClient,
            marketDataRepository
        );
    }

    @Test
    void shouldRejectDuplicateTimestampsInProviderResponse() {
        Issuer appleIssuer = new Issuer("Apple Inc.", "United States");
        Asset apple = new Asset("Apple", "AAPL", AssetClass.EQUITY, appleIssuer);

        LocalDate date = LocalDate.of(2026, 10, 1);

        MarketDataDTO firstDTO = new MarketDataDTO(
                250.0, 255.0, 249.0, 252.0, 100000.0, date
        );
        MarketDataDTO duplicateDTO = new MarketDataDTO(
                251.0, 256.0, 250.0, 253.0, 110000.0, date
        );

        when(assetService.findAssetBySymbol("AAPL"))
                .thenReturn(Optional.of(apple));

        when(marketDataClient.getHistoricalData(
                "AAPL", date, date, "1day"
        )).thenReturn(List.of(firstDTO, duplicateDTO));

        assertThrows(
                IllegalArgumentException.class,
                () -> marketDataService.fetchHistoricalData(
                        "AAPL", date, date, "1day"
                )
        );

        verify(marketDataRepository, never()).saveAll(org.mockito.ArgumentMatchers.anyList());
    }
}
