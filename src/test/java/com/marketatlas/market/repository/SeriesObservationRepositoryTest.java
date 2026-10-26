package com.marketatlas.market.repository;

import com.marketatlas.enums.CopyrightClass;
import com.marketatlas.enums.SeriesFrequency;
import com.marketatlas.enums.SeriesProvider;
import com.marketatlas.market.TestPostgresConfiguration;
import com.marketatlas.market.entity.DataSeries;
import com.marketatlas.market.entity.SeriesObservation;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

@SpringBootTest
@Import(TestPostgresConfiguration.class)
@Transactional
class SeriesObservationRepositoryTest {

    private static final LocalDate D1 = LocalDate.of(2026, 9, 1);
    private static final LocalDate D2 = LocalDate.of(2026, 9, 2);
    private static final LocalDate D3 = LocalDate.of(2026, 9, 3);

    @Autowired
    private DataSeriesRepository seriesRepository;

    @Autowired
    private SeriesObservationRepository observationRepository;

    @Autowired
    private EntityManager entityManager;

    private DataSeries newSeries(String externalId) {
        return new DataSeries(SeriesProvider.FRED, externalId, "Test series",
                SeriesFrequency.DAILY, "USD per barrel", "EIA",
                CopyrightClass.PUBLIC_DOMAIN_CITATION_REQUESTED, "Source: EIA, via FRED");
    }

    private List<SeriesObservation> read(DataSeries series, LocalDate from, LocalDate to) {
        entityManager.flush();
        entityManager.clear();
        return observationRepository
                .findBySeriesAndObservationDateBetweenOrderByObservationDateAsc(series, from, to);
    }

    @Test
    void shouldStoreMissingValueAsNullAndUpdateOnConflict() {
        DataSeries series = seriesRepository.save(newSeries("TEST1"));
        entityManager.flush();
        Instant first = Instant.parse("2026-10-26T00:00:00Z");

        int written = observationRepository.upsertBatch(series.getId(),
                new LocalDate[]{D3, D1, D2},
                new BigDecimal[]{new BigDecimal("3.25"), new BigDecimal("1.5"), null},
                first);
        assertEquals(3, written);

        List<SeriesObservation> rows = read(series, D1, D3);
        assertEquals(List.of(D1, D2, D3), rows.stream().map(SeriesObservation::getObservationDate).toList());
        assertEquals(0, new BigDecimal("1.5").compareTo(rows.get(0).getValue()));
        assertNull(rows.get(1).getValue());
        assertEquals(0, new BigDecimal("3.25").compareTo(rows.get(2).getValue()));

        Instant second = Instant.parse("2026-10-27T00:00:00Z");
        int rewritten = observationRepository.upsertBatch(series.getId(),
                new LocalDate[]{D2}, new BigDecimal[]{new BigDecimal("2.0")}, second);
        assertEquals(1, rewritten);

        rows = read(series, D1, D3);
        assertEquals(3, rows.size());
        assertEquals(0, new BigDecimal("2.0").compareTo(rows.get(1).getValue()));
        assertEquals(second, rows.get(1).getFetchedAt());
        assertEquals(first, rows.get(0).getFetchedAt());
    }

    @Test
    void shouldRejectDuplicateSeriesRegistration() {
        seriesRepository.saveAndFlush(newSeries("TEST2"));

        assertThrows(DataIntegrityViolationException.class,
                () -> seriesRepository.saveAndFlush(newSeries("TEST2")));
    }
}