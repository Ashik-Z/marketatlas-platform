package com.marketatlas.trading.service;

import com.marketatlas.common.exception.ResourceNotFoundException;
import com.marketatlas.market.entity.Asset;
import com.marketatlas.market.entity.MarketData;
import com.marketatlas.market.repository.AssetRepository;
import com.marketatlas.market.repository.MarketDataRepository;
import com.marketatlas.trading.dto.PortfolioResponse;
import com.marketatlas.trading.dto.PositionResponse;
import com.marketatlas.trading.model.Account;
import com.marketatlas.trading.model.Trade;
import com.marketatlas.trading.repository.AccountRepository;
import com.marketatlas.trading.repository.TradeRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Clock;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.TreeMap;

@Service
public class PortfolioService {

    private static final int SCALE = 6;

    private final AccountRepository accountRepository;
    private final TradeRepository tradeRepository;
    private final AssetRepository assetRepository;
    private final MarketDataRepository marketDataRepository;
    private final Clock clock;
    private final int maxPriceAgeDays;

    public PortfolioService(AccountRepository accountRepository,
                            TradeRepository tradeRepository,
                            AssetRepository assetRepository,
                            MarketDataRepository marketDataRepository,
                            Clock clock,
                            @Value("${marketatlas.trading.max-price-age-days:5}") int maxPriceAgeDays) {
        this.accountRepository = accountRepository;
        this.tradeRepository = tradeRepository;
        this.assetRepository = assetRepository;
        this.marketDataRepository = marketDataRepository;
        this.clock = clock;
        this.maxPriceAgeDays = maxPriceAgeDays;
    }

    @Transactional(readOnly = true)
    public PortfolioResponse getPortfolio(Long accountId) {
        Account account = accountRepository.findById(accountId)
                .orElseThrow(() -> new ResourceNotFoundException("Account not found: " + accountId));

        Map<Long, List<Trade>> tradesByAsset = new TreeMap<>();
        for (Trade trade : tradeRepository.findByAccountIdOrderByExecutedAtAscIdAsc(accountId)) {
            tradesByAsset.computeIfAbsent(trade.getAsset().getId(), id -> new ArrayList<>()).add(trade);
        }

        LocalDate today = LocalDate.now(clock);
        List<PositionResponse> positions = new ArrayList<>();
        BigDecimal positionsValue = BigDecimal.ZERO.setScale(SCALE);
        BigDecimal realized = BigDecimal.ZERO.setScale(SCALE);
        BigDecimal unrealized = BigDecimal.ZERO.setScale(SCALE);
        boolean complete = true;

        for (Map.Entry<Long, List<Trade>> entry : tradesByAsset.entrySet()) {
            var basis = CostBasisCalculator.calculate(entry.getValue());
            // Profit already booked counts even when the position is now fully sold.
            realized = realized.add(basis.realizedPnl());
            if (basis.quantity().signum() == 0) {
                continue;
            }

            Asset asset = assetRepository.findById(entry.getKey()).orElseThrow();
            Optional<MarketData> bar = marketDataRepository.findFirstByAssetOrderByTimestampDesc(asset);

            if (bar.isEmpty() || bar.get().getClose() == null || bar.get().getClose() <= 0) {
                // Cannot happen through normal use (an order needs a price), but never guess a value.
                complete = false;
                positions.add(new PositionResponse(asset.getId(), asset.getName(), basis.quantity(),
                        basis.averageCost(), basis.costBasis(), null, null, null, null, true));
                continue;
            }

            BigDecimal price = BigDecimal.valueOf(bar.get().getClose()).setScale(SCALE, RoundingMode.HALF_EVEN);
            BigDecimal value = price.multiply(basis.quantity()).setScale(SCALE, RoundingMode.HALF_EVEN);
            BigDecimal positionPnl = value.subtract(basis.costBasis());
            LocalDate priceDate = bar.get().getTimestamp();
            boolean stale = priceDate.isBefore(today.minusDays(maxPriceAgeDays));

            positionsValue = positionsValue.add(value);
            unrealized = unrealized.add(positionPnl);
            positions.add(new PositionResponse(asset.getId(), asset.getName(), basis.quantity(),
                    basis.averageCost(), basis.costBasis(), price, priceDate, value, positionPnl, stale));
        }

        BigDecimal total = account.getCashBalance().add(positionsValue);
        return new PortfolioResponse(account.getId(), account.getCurrency(),
                account.getStartingBalance(), account.getCashBalance(), positionsValue,
                total, total.subtract(account.getStartingBalance()), realized, unrealized,
                complete, positions);
    }
}