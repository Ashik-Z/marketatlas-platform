package com.marketatlas.ui.api;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/** Response of GET /api/accounts/{id}/portfolio. */
public record Portfolio(
        Long accountId,
        String currency,
        BigDecimal startingBalance,
        BigDecimal cashBalance,
        BigDecimal positionsValue,
        BigDecimal totalValue,
        BigDecimal profitLoss,
        BigDecimal realizedPnl,
        BigDecimal unrealizedPnl,
        boolean valuationComplete,
        List<Position> positions) {

    public record Position(Long assetId, String assetName, BigDecimal quantity, BigDecimal averageCost,
                           BigDecimal costBasis, BigDecimal latestPrice, LocalDate priceDate,
                           BigDecimal marketValue, BigDecimal unrealizedPnl, boolean stale) {
    }
}