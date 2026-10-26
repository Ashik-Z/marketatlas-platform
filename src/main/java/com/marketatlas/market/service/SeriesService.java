package com.marketatlas.market.service;

import com.marketatlas.common.exception.ResourceNotFoundException;
import com.marketatlas.market.client.SeriesClient;
import com.marketatlas.market.dto.SeriesObservationDTO;
import com.marketatlas.market.entity.DataSeries;
import com.marketatlas.market.repository.DataSeriesRepository;
import com.marketatlas.market.repository.SeriesObservationRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

@Service
public class SeriesService {

    private final DataSeriesRepository seriesRepository;
    private final SeriesObservationRepository observationRepository;
    private final SeriesClient seriesClient;
    private final Clock clock;
    private final TransactionTemplate transactionTemplate;

    public SeriesService(DataSeriesRepository seriesRepository,
                         SeriesObservationRepository observationRepository,
                         SeriesClient seriesClient,
                         Clock clock,
                         PlatformTransactionManager transactionManager) {
        this.seriesRepository = seriesRepository;
        this.observationRepository = observationRepository;
        this.seriesClient = seriesClient;
        this.clock = clock;
        this.transactionTemplate = new TransactionTemplate(transactionManager);
    }

    public List<DataSeries> listSeries() {
        return seriesRepository.findAll();
    }

    public List<SeriesObservationDTO> getObservations(Long seriesId, LocalDate startDate, LocalDate endDate) {
        validateRange(startDate, endDate);
        DataSeries series = findSeries(seriesId);
        return observationRepository
                .findBySeriesAndObservationDateBetweenOrderByObservationDateAsc(series, startDate, endDate)
                .stream()
                .map(o -> new SeriesObservationDTO(o.getObservationDate(), o.getValue()))
                .toList();
    }

    /**
     * Fetches the range from the provider and stores it, overwriting any existing values for
     * those dates. The provider call happens outside a transaction. Returns rows written.
     */
    public int refresh(Long seriesId, LocalDate startDate, LocalDate endDate) {
        validateRange(startDate, endDate);
        DataSeries series = findSeries(seriesId);

        LocalDate today = LocalDate.now(clock);
        LocalDate effectiveEnd = endDate.isAfter(today) ? today : endDate;
        if (startDate.isAfter(effectiveEnd)) {
            return 0;
        }

        List<SeriesObservationDTO> fetched =
                seriesClient.getObservations(series.getExternalId(), startDate, effectiveEnd);
        if (fetched.isEmpty()) {
            return 0;
        }

        Map<LocalDate, BigDecimal> byDate = new TreeMap<>();
        for (SeriesObservationDTO observation : fetched) {
            byDate.put(observation.date(), observation.value());
        }

        LocalDate[] dates = byDate.keySet().toArray(new LocalDate[0]);
        BigDecimal[] values = byDate.values().toArray(new BigDecimal[0]);
        Instant fetchedAt = clock.instant();

        Integer written = transactionTemplate.execute(status ->
                observationRepository.upsertBatch(series.getId(), dates, values, fetchedAt));
        return written == null ? 0 : written;
    }

    private DataSeries findSeries(Long seriesId) {
        return seriesRepository.findById(seriesId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Series " + seriesId + " was not found."));
    }

    private static void validateRange(LocalDate startDate, LocalDate endDate) {
        if (startDate == null || endDate == null) {
            throw new IllegalArgumentException("Start and end dates are required.");
        }
        if (startDate.isAfter(endDate)) {
            throw new IllegalArgumentException("Start date must not be after end date.");
        }
    }
}