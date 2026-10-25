package com.marketatlas.market.repository;

import com.marketatlas.enums.AssetClass;
import com.marketatlas.market.TestPostgresConfiguration;
import com.marketatlas.market.entity.Asset;
import com.marketatlas.market.entity.Issuer;
import com.marketatlas.market.entity.MarketData;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

@SpringBootTest
@Import(TestPostgresConfiguration.class)
@Transactional
class MarketDataBatchUpsertTest {

    @Autowired
    private MarketDataRepository marketDataRepository;

    @Autowired
    private AssetRepository assetRepository;

    @Autowired
    private IssuerRepository issuerRepository;

    @Autowired
    private EntityManager entityManager;

    @Test
    void shouldInsertThenUpdateInOneStatement() {
        Issuer issuer = issuerRepository.save(new Issuer("BatchUpsert Test Inc.", "Testland"));
        Asset asset = assetRepository.save(new Asset("BatchUpsert Test", AssetClass.EQUITY, issuer));
        entityManager.flush();

        LocalDate d1 = LocalDate.of(2026, 1, 5);
        LocalDate d2 = LocalDate.of(2026, 1, 6);
        LocalDate d3 = LocalDate.of(2026, 1, 7);

        // deliberately out of order
        int inserted = marketDataRepository.upsertBatch(asset.getId(),
                new LocalDate[]{d3, d1, d2},
                new Double[]{30.0, 10.0, 20.0},
                new Double[]{31.0, 11.0, 21.0},
                new Double[]{29.0, 9.0, 19.0},
                new Double[]{30.5, 10.5, 20.5},
                new Double[]{300.0, 100.0, 200.0});
        assertEquals(3, inserted);

        entityManager.clear();
        List<MarketData> rows = marketDataRepository
                .findByAssetAndTimestampBetweenOrderByTimestampAsc(asset, d1, d3);
        assertEquals(3, rows.size());
        assertEquals(d1, rows.get(0).getTimestamp());
        assertEquals(10.5, rows.get(0).getClose().doubleValue());
        assertEquals(300.0, rows.get(2).getVolume().doubleValue());

        // update d1 and d2, add d4
        LocalDate d4 = LocalDate.of(2026, 1, 8);
        int written = marketDataRepository.upsertBatch(asset.getId(),
                new LocalDate[]{d1, d2, d4},
                new Double[]{1.0, 2.0, 4.0},
                new Double[]{1.5, 2.5, 4.5},
                new Double[]{0.5, 1.5, 3.5},
                new Double[]{1.2, 2.2, 4.2},
                new Double[]{10.0, 20.0, 40.0});
        assertEquals(3, written);

        entityManager.clear();
        rows = marketDataRepository
                .findByAssetAndTimestampBetweenOrderByTimestampAsc(asset, d1, d4);
        assertEquals(4, rows.size());
        assertEquals(1.2, rows.get(0).getClose().doubleValue());
        assertEquals(30.5, rows.get(2).getClose().doubleValue());
    }
}