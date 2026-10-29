package com.marketatlas.market.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public record RealPriceResponse(
        Long assetId,
        String assetName,
        String currency,
        String cpiSeries,
        String cpiName,
        String attribution,
        LocalDate baseMonth,
        BigDecimal baseCpi,
        LocalDate latestCpiMonth,
        BigDecimal nominalReturnPct,
        BigDecimal realReturnPct,
        List<Point> points) {

    public record Point(LocalDate date, BigDecimal nominalClose, BigDecimal realClose,
                        LocalDate cpiMonth, boolean cpiEstimated) {
    }
}