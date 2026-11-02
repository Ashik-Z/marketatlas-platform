package com.marketatlas.watchlist.service;

import com.marketatlas.common.exception.ResourceNotFoundException;
import com.marketatlas.common.exception.WatchlistLimitException;
import com.marketatlas.market.entity.Asset;
import com.marketatlas.market.entity.MarketData;
import com.marketatlas.market.repository.AssetRepository;
import com.marketatlas.market.repository.MarketDataRepository;
import com.marketatlas.trading.service.Caller;
import com.marketatlas.watchlist.dto.WatchlistEntryResponse;
import com.marketatlas.watchlist.model.WatchlistItem;
import com.marketatlas.watchlist.repository.WatchlistItemRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

/** Personal watchlist: every operation is scoped to the caller, with no admin override. */
@Service
public class WatchlistService {

    public static final int MAX_ITEMS = 50;

    private final WatchlistItemRepository items;
    private final AssetRepository assets;
    private final MarketDataRepository marketData;

    public WatchlistService(WatchlistItemRepository items, AssetRepository assets, MarketDataRepository marketData) {
        this.items = items;
        this.assets = assets;
        this.marketData = marketData;
    }

    @Transactional(readOnly = true)
    public List<WatchlistEntryResponse> list(Caller caller) {
        return items.findByOwnerUsernameOrderByAddedAtDesc(caller.username()).stream()
                .map(this::entry)
                .toList();
    }

    /** Idempotent: adding an asset that is already watched changes nothing. */
    @Transactional
    public void add(Caller caller, Long assetId) {
        if (!assets.existsById(assetId)) {
            throw new ResourceNotFoundException("Asset not found: " + assetId);
        }
        if (!items.existsByOwnerUsernameAndAssetId(caller.username(), assetId)
                && items.countByOwnerUsername(caller.username()) >= MAX_ITEMS) {
            throw new WatchlistLimitException("Watchlist is full (" + MAX_ITEMS + " assets maximum).");
        }
        items.insertIfAbsent(caller.username(), assetId);
    }

    /** Idempotent: removing an asset that is not watched changes nothing. */
    @Transactional
    public void remove(Caller caller, Long assetId) {
        items.deleteByOwnerUsernameAndAssetId(caller.username(), assetId);
    }

    private WatchlistEntryResponse entry(WatchlistItem item) {
        Asset asset = assets.findById(item.getAssetId()).orElseThrow();
        List<MarketData> bars = marketData.findTop2ByAssetOrderByTimestampDesc(asset);
        MarketData latest = bars.isEmpty() ? null : bars.get(0);
        MarketData previous = bars.size() > 1 ? bars.get(1) : null;

        BigDecimal latestClose = latest == null ? null : BigDecimal.valueOf(latest.getClose());
        BigDecimal previousClose = previous == null ? null : BigDecimal.valueOf(previous.getClose());
        BigDecimal change = null;
        BigDecimal changePercent = null;
        if (latestClose != null && previousClose != null) {
            change = latestClose.subtract(previousClose).setScale(4, RoundingMode.HALF_UP);
            if (previousClose.signum() != 0) {
                changePercent = latestClose.subtract(previousClose)
                        .multiply(BigDecimal.valueOf(100))
                        .divide(previousClose, 4, RoundingMode.HALF_UP);
            }
        }
        return new WatchlistEntryResponse(asset.getId(), asset.getName(), String.valueOf(asset.getAssetClass()),
                asset.getCurrency(), asset.getUnit(), latestClose,
                latest == null ? null : latest.getTimestamp(), previousClose, change, changePercent,
                item.getAddedAt());
    }
}
