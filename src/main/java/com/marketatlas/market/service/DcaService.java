package com.marketatlas.market.service;

import com.marketatlas.common.exception.ResourceNotFoundException;
import com.marketatlas.enums.SeriesProvider;
import com.marketatlas.market.dto.DcaResponse;
import com.marketatlas.market.entity.Asset;
import com.marketatlas.market.repository.AssetRepository;
import com.marketatlas.market.repository.DataSeriesRepository;
import com.marketatlas.market.repository.MarketDataRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.NavigableMap;

@Service
public class DcaService {

    private static final long MAX_RANGE_DAYS = 3660;
    private static final BigDecimal MAX_AMOUNT = new BigDecimal("1000000000");

    private final AssetRepository assetRepository;
    private final MarketDataRepository marketDataRepository;
    private final DataSeriesRepository dataSeriesRepository;
    private final RealPriceService realPriceService;

    public DcaService(AssetRepository assetRepository, MarketDataRepository marketDataRepository,
                      DataSeriesRepository dataSeriesRepository, RealPriceService realPriceService) {
        this.assetRepository = assetRepository;
        this.marketDataRepository = marketDataRepository;
        this.dataSeriesRepository = dataSeriesRepository;
        this.realPriceService = realPriceService;
    }

    /** Database only: never calls a provider. */
    @Transactional(readOnly = true)
    public DcaResponse run(Long assetId, LocalDate startDate, LocalDate endDate, BigDecimal amount,
                           YearMonth baseMonth) {
        if (startDate.isAfter(endDate)) {
            throw new IllegalArgumentException("startDate must be before or equal to endDate");
        }
        if (ChronoUnit.DAYS.between(startDate, endDate) > MAX_RANGE_DAYS) {
            throw new IllegalArgumentException("The date range must not exceed " + MAX_RANGE_DAYS + " days");
        }
        if (amount.signum() <= 0 || amount.compareTo(MAX_AMOUNT) > 0 || amount.stripTrailingZeros().scale() > 2) {
            throw new IllegalArgumentException(
                    "amount must be greater than zero, at most " + MAX_AMOUNT.toPlainString()
                            + ", with at most 2 decimal places");
        }

        Asset asset = assetRepository.findById(assetId)
                .orElseThrow(() -> new ResourceNotFoundException("Asset not found: " + assetId));
        if (!"USD".equals(asset.getCurrency())) {
            throw new IllegalArgumentException("Real-terms figures use US CPI, so they are only available for USD assets");
        }

        NavigableMap<LocalDate, BigDecimal> cpiByMonth = realPriceService.cpiByMonthOrEmpty();
        if (cpiByMonth.isEmpty()) {
            throw new ResourceNotFoundException(
                    "No " + RealPriceService.CPI_SERIES_ID + " observations are loaded yet");
        }
        LocalDate latestCpiMonth = cpiByMonth.lastKey();
        LocalDate base = baseMonth == null ? latestCpiMonth : baseMonth.atDay(1);

        List<RealPriceCalculator.Bar> bars = marketDataRepository
                .findByAssetAndTimestampBetweenOrderByTimestampAsc(asset, startDate, endDate).stream()
                .map(m -> new RealPriceCalculator.Bar(m.getTimestamp(), m.getClose()))
                .toList();
        DcaCalculator.Result r = DcaCalculator.run(bars, cpiByMonth, base, amount);

        var series = dataSeriesRepository
                .findByProviderAndExternalId(SeriesProvider.FRED, RealPriceService.CPI_SERIES_ID);
        return new DcaResponse(
                asset.getId(), asset.getName(), asset.getCurrency(),
                RealPriceService.CPI_SERIES_ID, series.map(s -> s.getAttribution()).orElse(null),
                base, latestCpiMonth, amount, r.purchases().size(),
                r.investedNominal(), r.investedReal(), r.units(),
                r.valuationDate(), r.valuationClose(), r.valueNominal(), r.valueReal(),
                r.nominalGain(), r.realGain(), r.nominalGainPct(), r.realGainPct(),
                r.purchases().stream()
                        .map(p -> new DcaResponse.Purchase(p.date(), p.close(), p.units(), p.amount(),
                                p.realAmount(), p.cpiEstimated()))
                        .toList());
    }
}
