package com.marketatlas.ui.api;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/** Response of GET /api/assets/{id}/real-prices (inflation-adjusted closes). */
public record RealPrices(
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