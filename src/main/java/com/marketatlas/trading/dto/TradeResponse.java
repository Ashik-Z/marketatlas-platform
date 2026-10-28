package com.marketatlas.trading.dto;

import com.marketatlas.trading.model.Trade;
import com.marketatlas.trading.model.TradeSide;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

public record TradeResponse(
        Long id,
        Long accountId,
        Long assetId,
        TradeSide side,
        BigDecimal quantity,
        BigDecimal price,
        BigDecimal commission,
        LocalDate priceDate,
        Instant executedAt) {

    public static TradeResponse from(Trade trade) {
        return new TradeResponse(trade.getId(), trade.getAccount().getId(), trade.getAsset().getId(),
                trade.getSide(), trade.getQuantity(), trade.getPrice(), trade.getCommission(),
                trade.getPriceDate(), trade.getExecutedAt());
    }
}