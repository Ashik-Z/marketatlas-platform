package com.marketatlas.market.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public record DcaResponse(
        Long assetId,
        String assetName,
        String currency,
        String cpiSeries,
        String attribution,
        LocalDate baseMonth,
        LocalDate latestCpiMonth,
        BigDecimal amountPerPurchase,
        int purchaseCount,
        BigDecimal investedNominal,
        BigDecimal investedReal,
        BigDecimal units,
        LocalDate valuationDate,
        BigDecimal valuationClose,
        BigDecimal valueNominal,
        BigDecimal valueReal,
        BigDecimal nominalGain,
        BigDecimal realGain,
        BigDecimal nominalGainPct,
        BigDecimal realGainPct,
        List<Purchase> purchases) {

    public record Purchase(LocalDate date, BigDecimal close, BigDecimal units, BigDecimal amount,
                           BigDecimal realAmount, boolean cpiEstimated) {
    }
}
