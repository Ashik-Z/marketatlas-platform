package com.marketatlas.market.service;

import com.marketatlas.market.entity.Asset;
import com.marketatlas.market.entity.Issuer;
import com.marketatlas.market.entity.MarketData;
import com.marketatlas.market.repository.MarketDataRepository;
import com.marketatlas.enums.AssetClass;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MarketDataServiceTest {

    @Mock
    private MarketDataRepository marketDataRepository;

    private MarketDataServiceImpl marketDataService;

    @BeforeEach
    void setUp() {
        marketDataService =
            new MarketDataServiceImpl(marketDataRepository);
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
            Instant.parse("2026-10-01T10:00:00Z")
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
            Instant.parse("2026-10-01T10:00:00Z")
        );

        MarketData secondObservation = new MarketData(
            255.10,
            255.50,
            254.90,
            255.42,
            142310.0,
            apple,
            Instant.parse("2026-10-01T10:05:00Z")
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
            Instant.parse("2026-10-01T10:00:00Z")
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
}
