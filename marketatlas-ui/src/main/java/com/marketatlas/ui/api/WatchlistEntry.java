package com.marketatlas.ui.api;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

/** One watched asset with its latest close and nominal change versus the previous bar (nulls when unknown). */
public record WatchlistEntry(
        Long assetId,
        String name,
        String assetClass,
        String currency,
        String unit,
        BigDecimal latestClose,
        LocalDate latestDate,
        BigDecimal previousClose,
        BigDecimal change,
        BigDecimal changePercent,
        Instant addedAt) {
}
