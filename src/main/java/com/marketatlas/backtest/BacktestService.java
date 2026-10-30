package com.marketatlas.backtest;

import com.marketatlas.common.exception.ResourceNotFoundException;
import com.marketatlas.enums.AssetClass;
import com.marketatlas.market.entity.Asset;
import com.marketatlas.market.entity.MarketData;
import com.marketatlas.market.repository.AssetRepository;
import com.marketatlas.market.repository.MarketDataRepository;
import com.marketatlas.market.service.RealPriceService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;

/** Runs a backtest on stored daily bars only; never calls a market-data provider. */
@Service
public class BacktestService {

    private static final long MAX_RANGE_DAYS = 3660;

    private final AssetRepository assetRepository;
    private final MarketDataRepository marketDataRepository;
    private final RealPriceService realPriceService;
    private final BigDecimal defaultCommission;

    public BacktestService(AssetRepository assetRepository,
                           MarketDataRepository marketDataRepository,
                           RealPriceService realPriceService,
                           @Value("${marketatlas.trading.commission:1.00}") BigDecimal defaultCommission) {
        this.assetRepository = assetRepository;
        this.marketDataRepository = marketDataRepository;
        this.realPriceService = realPriceService;
        this.defaultCommission = defaultCommission;
    }

    @Transactional(readOnly = true)
    public BacktestResponse run(Long assetId, BacktestRequest request) {
        if (request.startDate().isAfter(request.endDate())) {
            throw new IllegalArgumentException("startDate must be before or equal to endDate");
        }
        if (ChronoUnit.DAYS.between(request.startDate(), request.endDate()) > MAX_RANGE_DAYS) {
            throw new IllegalArgumentException("The date range must not exceed " + MAX_RANGE_DAYS + " days");
        }

        Asset asset = assetRepository.findById(assetId)
                .orElseThrow(() -> new ResourceNotFoundException("Asset not found: " + assetId));

        List<Backtest.Bar> bars = new ArrayList<>();
        int skipped = 0;
        for (MarketData stored : marketDataRepository.findByAssetAndTimestampBetweenOrderByTimestampAsc(
                asset, request.startDate(), request.endDate())) {
            DayOfWeek day = stored.getTimestamp().getDayOfWeek();
            boolean weekend = day == DayOfWeek.SATURDAY || day == DayOfWeek.SUNDAY;
            if (weekend || !usable(stored.getOpen()) || !usable(stored.getClose())) {
                skipped++;
                continue;
            }
            bars.add(new Backtest.Bar(stored.getTimestamp(),
                    BigDecimal.valueOf(stored.getOpen()), BigDecimal.valueOf(stored.getClose())));
        }
        if (bars.size() < 2) {
            throw new IllegalArgumentException(
                    "At least two stored daily bars are needed in that range; fetch the history first");
        }

        BigDecimal commission = request.commission() != null ? request.commission() : defaultCommission;
        int slippage = request.slippageBps() != null ? request.slippageBps() : 0;
        Backtest.Params params = new Backtest.Params(request.initialCash(), commission, slippage,
                asset.getAssetClass() == AssetClass.EQUITY);

        Strategy strategy = strategyFor(request);
        Backtest.Result result = BacktestEngine.run(bars, strategy, params);
        Backtest.Result benchmark = BacktestEngine.run(bars, Strategies.buyAndHold(), params);

        RealTermsCalculator.RealTerms realTerms = null;
        String realTermsNote = null;
        if (!"USD".equals(asset.getCurrency())) {
            realTermsNote = "Real terms use US CPI, so they are only available for USD assets";
        } else {
            realTerms = RealTermsCalculator.compute(result.equityCurve(), benchmark.equityCurve(),
                    realPriceService.cpiByMonthOrEmpty());
            if (realTerms == null) {
                realTermsNote = "No CPIAUCNS data at or before the first bar; refresh the series first";
            }
        }

        boolean curves = Boolean.TRUE.equals(request.includeEquityCurve());
        return new BacktestResponse(
                asset.getId(), asset.getName(), asset.getCurrency(), strategy.name(),
                bars.getFirst().date(), bars.getLast().date(), bars.size(), skipped,
                request.initialCash(), commission, slippage,
                BacktestMetrics.compute(result), BacktestMetrics.compute(benchmark),
                result.trades().stream()
                        .map(t -> new BacktestResponse.TradeView(t.date(), t.side().name(), t.quantity(),
                                t.price(), t.commission()))
                        .toList(),
                curves ? curve(result) : null,
                curves ? curve(benchmark) : null,
                realTerms, realTermsNote);
    }

    private static boolean usable(Double price) {
        return price != null && Double.isFinite(price) && price > 0;
    }

    private static Strategy strategyFor(BacktestRequest request) {
        return switch (request.strategy()) {
            case BUY_AND_HOLD -> Strategies.buyAndHold();
            case SMA_CROSSOVER -> {
                if (request.shortWindow() == null || request.longWindow() == null) {
                    throw new IllegalArgumentException(
                            "shortWindow and longWindow are required for SMA_CROSSOVER");
                }
                yield Strategies.smaCrossover(request.shortWindow(), request.longWindow());
            }
        };
    }

    private static List<BacktestResponse.CurvePoint> curve(Backtest.Result result) {
        return result.equityCurve().stream()
                .map(p -> new BacktestResponse.CurvePoint(p.date(), p.equity()))
                .toList();
    }
}