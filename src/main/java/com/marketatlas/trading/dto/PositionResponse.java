package com.marketatlas.trading.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

public record PositionResponse(
        Long assetId,
        String assetName,
        BigDecimal quantity,
        BigDecimal averageCost,
        BigDecimal costBasis,
        BigDecimal latestPrice,
        LocalDate priceDate,
        BigDecimal marketValue,
        BigDecimal unrealizedPnl,
        boolean stale) {
}