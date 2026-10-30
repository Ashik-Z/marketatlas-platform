package com.marketatlas.market.service;

import com.marketatlas.common.exception.ResourceNotFoundException;
import com.marketatlas.enums.SeriesProvider;
import com.marketatlas.market.dto.RealPriceResponse;
import com.marketatlas.market.entity.Asset;
import com.marketatlas.market.entity.DataSeries;
import com.marketatlas.market.repository.AssetRepository;
import com.marketatlas.market.repository.DataSeriesRepository;
import com.marketatlas.market.repository.MarketDataRepository;
import com.marketatlas.market.repository.SeriesObservationRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.NavigableMap;
import java.util.TreeMap;

@Service
public class RealPriceService {

    static final String CPI_SERIES_ID = "CPIAUCNS";
    private static final long MAX_RANGE_DAYS = 3660;

    private final AssetRepository assetRepository;
    private final MarketDataRepository marketDataRepository;
    private final DataSeriesRepository dataSeriesRepository;
    private final SeriesObservationRepository observationRepository;

    public RealPriceService(AssetRepository assetRepository,
                            MarketDataRepository marketDataRepository,
                            DataSeriesRepository dataSeriesRepository,
                            SeriesObservationRepository observationRepository) {
        this.assetRepository = assetRepository;
        this.marketDataRepository = marketDataRepository;
        this.dataSeriesRepository = dataSeriesRepository;
        this.observationRepository = observationRepository;
    }

    /** Database only: never calls a provider. */
    @Transactional(readOnly = true)
    public RealPriceResponse getRealPrices(Long assetId, LocalDate startDate, LocalDate endDate,
                                           YearMonth baseMonth) {
        if (startDate.isAfter(endDate)) {
            throw new IllegalArgumentException("startDate must be before or equal to endDate");
        }
        if (ChronoUnit.DAYS.between(startDate, endDate) > MAX_RANGE_DAYS) {
            throw new IllegalArgumentException("The date range must not exceed " + MAX_RANGE_DAYS + " days");
        }

        Asset asset = assetRepository.findById(assetId)
                .orElseThrow(() -> new ResourceNotFoundException("Asset not found: " + assetId));
        if (!"USD".equals(asset.getCurrency())) {
            throw new IllegalArgumentException(
                    "Real prices use US CPI, so they are only available for USD assets");
        }

        DataSeries series = dataSeriesRepository.findByProviderAndExternalId(SeriesProvider.FRED, CPI_SERIES_ID)
                .orElseThrow(() -> new ResourceNotFoundException("Inflation series " + CPI_SERIES_ID + " is not available"));

        NavigableMap<LocalDate, BigDecimal> cpiByMonth = readCpi(series);
        if (cpiByMonth.isEmpty()) {
            throw new ResourceNotFoundException("No " + CPI_SERIES_ID + " observations are loaded yet");
        }

        LocalDate latestCpiMonth = cpiByMonth.lastKey();
        LocalDate base = baseMonth == null ? latestCpiMonth : baseMonth.atDay(1);

        List<RealPriceCalculator.Bar> bars = marketDataRepository
                .findByAssetAndTimestampBetweenOrderByTimestampAsc(asset, startDate, endDate).stream()
                .map(m -> new RealPriceCalculator.Bar(m.getTimestamp(), m.getClose()))
                .toList();
        List<RealPriceCalculator.Point> points = RealPriceCalculator.adjust(bars, cpiByMonth, base);

        List<RealPriceCalculator.Point> comparable = points.stream().filter(p -> p.realClose() != null).toList();
        BigDecimal nominalReturn = null;
        BigDecimal realReturn = null;
        if (comparable.size() >= 2) {
            var first = comparable.get(0);
            var last = comparable.get(comparable.size() - 1);
            nominalReturn = returnPct(first.nominalClose(), last.nominalClose());
            realReturn = returnPct(first.realClose(), last.realClose());
        }

        return new RealPriceResponse(
                asset.getId(), asset.getName(), asset.getCurrency(),
                series.getExternalId(), series.getName(), series.getAttribution(),
                base, cpiByMonth.get(base), latestCpiMonth, nominalReturn, realReturn,
                points.stream()
                        .map(p -> new RealPriceResponse.Point(p.date(), p.nominalClose(), p.realClose(),
                                p.cpiMonth(), p.cpiEstimated()))
                        .toList());
    }

    private NavigableMap<LocalDate, BigDecimal> readCpi(DataSeries series) {
        NavigableMap<LocalDate, BigDecimal> cpiByMonth = new TreeMap<>();
        observationRepository.findBySeriesOrderByObservationDateAsc(series).forEach(o -> {
            if (o.getValue() != null && o.getValue().signum() > 0) {
                cpiByMonth.put(o.getObservationDate().withDayOfMonth(1), o.getValue());
            }
        });
        return cpiByMonth;
    }

    /** Monthly CPI keyed by month start; empty when the series or its observations are not loaded. */
    @Transactional(readOnly = true)
    public NavigableMap<LocalDate, BigDecimal> cpiByMonthOrEmpty() {
        return dataSeriesRepository.findByProviderAndExternalId(SeriesProvider.FRED, CPI_SERIES_ID)
                .map(this::readCpi).orElseGet(TreeMap::new);
    }

    private static BigDecimal returnPct(BigDecimal first, BigDecimal last) {
        if (first.signum() == 0) {
            return null;
        }
        return last.divide(first, 10, RoundingMode.HALF_EVEN)
                .subtract(BigDecimal.ONE)
                .multiply(BigDecimal.valueOf(100))
                .setScale(4, RoundingMode.HALF_EVEN);
    }
}