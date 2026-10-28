package com.marketatlas.trading.service;

import com.marketatlas.common.exception.OrderRejectedException;
import com.marketatlas.common.exception.ResourceNotFoundException;
import com.marketatlas.market.entity.Asset;
import com.marketatlas.market.entity.MarketData;
import com.marketatlas.market.repository.AssetRepository;
import com.marketatlas.market.repository.MarketDataRepository;
import com.marketatlas.trading.model.Account;
import com.marketatlas.trading.model.Trade;
import com.marketatlas.trading.model.TradeSide;
import com.marketatlas.trading.repository.AccountRepository;
import com.marketatlas.trading.repository.TradeRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Clock;
import java.time.LocalDate;

@Service
public class OrderService {

    private static final int SCALE = 6;

    private final AccountRepository accountRepository;
    private final AssetRepository assetRepository;
    private final MarketDataRepository marketDataRepository;
    private final TradeRepository tradeRepository;
    private final Clock clock;
    private final int maxPriceAgeDays;
    private final BigDecimal commission;

    public OrderService(AccountRepository accountRepository,
                        AssetRepository assetRepository,
                        MarketDataRepository marketDataRepository,
                        TradeRepository tradeRepository,
                        Clock clock,
                        @Value("${marketatlas.trading.max-price-age-days:5}") int maxPriceAgeDays,
                        @Value("${marketatlas.trading.commission:1.00}") BigDecimal commission) {
        this.accountRepository = accountRepository;
        this.assetRepository = assetRepository;
        this.marketDataRepository = marketDataRepository;
        this.tradeRepository = tradeRepository;
        this.clock = clock;
        this.maxPriceAgeDays = maxPriceAgeDays;
        this.commission = commission.setScale(SCALE, RoundingMode.HALF_EVEN);
    }

    @Transactional
    public Trade placeMarketOrder(Long accountId, Long assetId, TradeSide side, BigDecimal quantity) {
        if (quantity == null || quantity.signum() <= 0) {
            throw new IllegalArgumentException("Quantity must be positive");
        }

        // The row lock is taken first and held to the end of the transaction, so concurrent
        // orders on one account queue up and always see each other's cash and positions.
        Account account = accountRepository.findByIdForUpdate(accountId)
                .orElseThrow(() -> new ResourceNotFoundException("Account not found: " + accountId));
        Asset asset = assetRepository.findById(assetId)
                .orElseThrow(() -> new ResourceNotFoundException("Asset not found: " + assetId));

        if (!account.getCurrency().equals(asset.getCurrency())) {
            throw new OrderRejectedException("Asset currency " + asset.getCurrency()
                    + " does not match account currency " + account.getCurrency());
        }
        if ("EQUITY".equals(asset.getAssetClass().name())
                && quantity.stripTrailingZeros().scale() > 0) {
            throw new OrderRejectedException("Equities trade in whole shares only");
        }

        MarketData bar = marketDataRepository.findFirstByAssetOrderByTimestampDesc(asset)
                .orElseThrow(() -> new OrderRejectedException("No price data is stored for this asset"));
        LocalDate today = LocalDate.now(clock);
        if (bar.getTimestamp().isBefore(today.minusDays(maxPriceAgeDays))) {
            throw new OrderRejectedException("Latest price is from " + bar.getTimestamp()
                    + ", older than " + maxPriceAgeDays + " days");
        }
        if (bar.getClose() == null || bar.getClose() <= 0) {
            throw new OrderRejectedException("Latest stored price is unusable");
        }

        BigDecimal price = BigDecimal.valueOf(bar.getClose()).setScale(SCALE, RoundingMode.HALF_EVEN);
        BigDecimal gross = price.multiply(quantity).setScale(SCALE, RoundingMode.HALF_EVEN);

        if (side == TradeSide.BUY) {
            BigDecimal cost = gross.add(commission);
            if (account.getCashBalance().compareTo(cost) < 0) {
                throw new OrderRejectedException("Insufficient cash: need " + cost
                        + " but the account has " + account.getCashBalance());
            }
            account.debit(cost);
        } else {
            BigDecimal held = tradeRepository.netQuantity(accountId, assetId);
            if (held == null) {
                held = BigDecimal.ZERO;
            }
            if (held.compareTo(quantity) < 0) {
                throw new OrderRejectedException("Insufficient position: holding " + held
                        + " but tried to sell " + quantity);
            }
            BigDecimal proceeds = gross.subtract(commission);
            if (proceeds.signum() < 0) {
                throw new OrderRejectedException("Sale proceeds do not cover the commission");
            }
            account.credit(proceeds);
        }

        return tradeRepository.save(new Trade(account, asset, side,
                quantity.setScale(SCALE, RoundingMode.HALF_EVEN), price, commission,
                bar.getTimestamp(), clock.instant()));
    }
}