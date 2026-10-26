package com.marketatlas.market.service;

import com.marketatlas.common.exception.MarketDataProviderException;
import com.marketatlas.common.exception.ResourceNotFoundException;
import com.marketatlas.enums.CopyrightClass;
import com.marketatlas.enums.SeriesProvider;
import com.marketatlas.market.TestPostgresConfiguration;
import com.marketatlas.market.client.SeriesClient;
import com.marketatlas.market.dto.SeriesObservationDTO;
import com.marketatlas.market.entity.DataSeries;
import com.marketatlas.market.repository.DataSeriesRepository;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@SpringBootTest
@Import(TestPostgresConfiguration.class)
@Transactional
class SeriesServiceTest {

    private static final LocalDate START = LocalDate.of(2026, 9, 1);
    private static final LocalDate END = LocalDate.of(2026, 9, 3);

    @Autowired
    private SeriesService seriesService;

    @Autowired
    private DataSeriesRepository seriesRepository;

    @Autowired
    private EntityManager entityManager;

    @Autowired
    private Clock clock;

    @MockitoBean
    private SeriesClient seriesClient;

    private Long gasId;

    @BeforeEach
    void setUp() {
        gasId = seriesRepository.findByProviderAndExternalId(SeriesProvider.FRED, "DHHNGSP")
                .orElseThrow().getId();
    }

    private static SeriesObservationDTO obs(LocalDate date, String value) {
        return new SeriesObservationDTO(date, value == null ? null : new BigDecimal(value));
    }

    @Test
    void shouldSeedTheFourPublicDomainSeries() {
        List<DataSeries> series = seriesService.listSeries();

        List<String> ids = series.stream().map(DataSeries::getExternalId).toList();
        assertTrue(ids.containsAll(List.of("CPIAUCSL", "CPIAUCNS", "DCOILBRENTEU", "DHHNGSP")));
        series.stream()
                .filter(s -> List.of("CPIAUCSL", "CPIAUCNS", "DCOILBRENTEU", "DHHNGSP")
                        .contains(s.getExternalId()))
                .forEach(s -> assertEquals(CopyrightClass.PUBLIC_DOMAIN_CITATION_REQUESTED, s.getCopyrightClass()));
    }

    @Test
    void shouldStoreFetchedObservationsIncludingMissingValues() {
        when(seriesClient.getObservations("DHHNGSP", START, END)).thenReturn(List.of(
                obs(START, "3.0"), obs(START.plusDays(1), null), obs(END, "3.25")));

        int written = seriesService.refresh(gasId, START, END);
        entityManager.flush();
        entityManager.clear();

        assertEquals(3, written);
        List<SeriesObservationDTO> stored = seriesService.getObservations(gasId, START, END);
        assertEquals(List.of(START, START.plusDays(1), END), stored.stream().map(SeriesObservationDTO::date).toList());
        assertNull(stored.get(1).value());
        assertEquals(0, new BigDecimal("3.25").compareTo(stored.get(2).value()));
    }

    @Test
    void shouldOverwriteExistingValuesOnRefetch() {
        when(seriesClient.getObservations("DHHNGSP", START, END))
                .thenReturn(List.of(obs(START, "3.0")))
                .thenReturn(List.of(obs(START, "3.5")));

        seriesService.refresh(gasId, START, END);
        seriesService.refresh(gasId, START, END);
        entityManager.flush();
        entityManager.clear();

        List<SeriesObservationDTO> stored = seriesService.getObservations(gasId, START, END);
        assertEquals(1, stored.size());
        assertEquals(0, new BigDecimal("3.5").compareTo(stored.get(0).value()));
    }

    @Test
    void shouldNotAskProviderForFutureDates() {
        LocalDate today = LocalDate.now(clock);
        when(seriesClient.getObservations("DHHNGSP", today.minusDays(5), today)).thenReturn(List.of());

        int written = seriesService.refresh(gasId, today.minusDays(5), today.plusDays(30));

        assertEquals(0, written);
        verify(seriesClient).getObservations("DHHNGSP", today.minusDays(5), today);
    }

    @Test
    void shouldWriteNothingWhenProviderFails() {
        when(seriesClient.getObservations("DHHNGSP", START, END))
                .thenThrow(new MarketDataProviderException("FRED rate limit exceeded (HTTP 429)."));

        assertThrows(MarketDataProviderException.class, () -> seriesService.refresh(gasId, START, END));
        entityManager.clear();

        assertTrue(seriesService.getObservations(gasId, START, END).isEmpty());
    }

    @Test
    void shouldRejectUnknownSeriesAndInvalidRange() {
        assertThrows(ResourceNotFoundException.class, () -> seriesService.refresh(-1L, START, END));
        assertThrows(IllegalArgumentException.class, () -> seriesService.refresh(gasId, END, START));
    }
}