package com.marketatlas.market.service;

import com.marketatlas.market.TestPostgresConfiguration;
import com.marketatlas.market.repository.AssetRepository;
import com.marketatlas.market.repository.IssuerRepository;
import com.marketatlas.market.repository.MarketDataRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.transaction.PlatformTransactionManager;

import com.marketatlas.enums.AssetClass;
import com.marketatlas.market.dto.MarketDataDTO;
import com.marketatlas.market.entity.Asset;
import com.marketatlas.market.entity.Issuer;
import com.marketatlas.market.entity.MarketData;

import java.time.LocalDate;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import org.springframework.transaction.support.TransactionTemplate;
import org.junit.jupiter.api.AfterEach;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@Import(TestPostgresConfiguration.class)
class MarketDataPersistenceConcurrencyTest {

    @Autowired
    private MarketDataPersistenceService marketDataPersistenceService;

    @Autowired
    private MarketDataRepository marketDataRepository;

    @Autowired
    private AssetRepository assetRepository;

    @Autowired
    private IssuerRepository issuerRepository;

    @Autowired
    private PlatformTransactionManager transactionManager;

    @Test
    void shouldHandleConcurrentUpsertsForSameAssetAndTimestamp() throws Exception {
        Issuer issuer = issuerRepository.saveAndFlush(
                new Issuer("Concurrency Test Inc.", "United States")
        );

        Asset asset = assetRepository.saveAndFlush(
                new Asset("Concurrency Test", "CTST", AssetClass.EQUITY, issuer)
        );

        Long assetId = asset.getId();
        LocalDate timestamp = LocalDate.of(2026, 10, 15);

        MarketDataDTO firstUpdate = new MarketDataDTO(
                100.0, 110.0, 95.0, 105.0, 1000.0, timestamp
        );

        MarketDataDTO secondUpdate = new MarketDataDTO(
                200.0, 210.0, 195.0, 205.0, 2000.0, timestamp
        );

        CountDownLatch ready = new CountDownLatch(2);
        CountDownLatch start = new CountDownLatch(1);

        ExecutorService executor = Executors.newFixedThreadPool(2);

        try {
            Future<?> first = executor.submit(() -> {
                ready.countDown();
                if (!start.await(10, TimeUnit.SECONDS)) {
                    throw new IllegalStateException("Timed out waiting to start");
                }

                TransactionTemplate transaction =
                        new TransactionTemplate(transactionManager);

                transaction.executeWithoutResult(status -> {
                    Asset workerAsset = assetRepository.findById(assetId)
                            .orElseThrow();

                    marketDataPersistenceService.persistHistoricalData(
                            workerAsset, List.of(firstUpdate)
                    );
                });

                return null;
            });

            Future<?> second = executor.submit(() -> {
                ready.countDown();
                if (!start.await(10, TimeUnit.SECONDS)) {
                    throw new IllegalStateException("Timed out waiting to start");
                }

                TransactionTemplate transaction =
                        new TransactionTemplate(transactionManager);

                transaction.executeWithoutResult(status -> {
                    Asset workerAsset = assetRepository.findById(assetId)
                            .orElseThrow();

                    marketDataPersistenceService.persistHistoricalData(
                            workerAsset, List.of(secondUpdate)
                    );
                });

                return null;
            });

            assertTrue(ready.await(10, TimeUnit.SECONDS),
                    "Workers did not become ready in time");

            start.countDown();

            first.get(30, TimeUnit.SECONDS);
            second.get(30, TimeUnit.SECONDS);
        } finally {
            start.countDown();
            executor.shutdownNow();
            assertTrue(executor.awaitTermination(10, TimeUnit.SECONDS),
                    "Worker threads did not terminate in time");
        }

        List<MarketData> records =
                marketDataRepository.findByAssetAndTimestampIn(
                        asset, List.of(timestamp)
                );

        assertEquals(1, records.size(),
                "Concurrent upserts must leave exactly one row");

        MarketData saved = records.get(0);

        assertTrue(
                (saved.getOpen().equals(100.0)
                        && saved.getHigh().equals(110.0)
                        && saved.getLow().equals(95.0)
                        && saved.getClose().equals(105.0)
                        && saved.getVolume().equals(1000.0))
                        ||
                        (saved.getOpen().equals(200.0)
                                && saved.getHigh().equals(210.0)
                                && saved.getLow().equals(195.0)
                                && saved.getClose().equals(205.0)
                                && saved.getVolume().equals(2000.0)),
                "The stored OHLCV record should match one complete update"
        );
    }

    @AfterEach
    void cleanUp() {
        assetRepository.findBySymbol("CTST").ifPresent(asset -> {
            marketDataRepository.deleteAll(
                    marketDataRepository.findByAsset(asset)
            );
            assetRepository.delete(asset);
        });

        issuerRepository.findAll().stream()
                .filter(issuer -> issuer.getName()
                        .equals("Concurrency Test Inc."))
                .forEach(issuerRepository::delete);
    }
}