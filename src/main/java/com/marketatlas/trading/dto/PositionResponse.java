package com.marketatlas.trading.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

public record PositionResponse(
        Long assetId,
        String assetName,
        BigDecimal quantity,
        BigDecimal latestPrice,
        LocalDate priceDate,
        BigDecimal marketValue,
        boolean stale) {
}