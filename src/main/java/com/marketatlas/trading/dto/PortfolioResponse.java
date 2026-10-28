package com.marketatlas.trading.dto;

import java.math.BigDecimal;
import java.util.List;

public record PortfolioResponse(
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
        List<PositionResponse> positions) {
}