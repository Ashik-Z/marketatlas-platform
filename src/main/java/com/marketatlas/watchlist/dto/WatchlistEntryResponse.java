package com.marketatlas.watchlist.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

/**
 * One watched asset with its latest stored close and the nominal change versus the previous stored bar.
 * Price fields are null when the asset has no stored bars; change fields are null with fewer than two bars.
 * Inflation-adjusted figures are intentionally not included: CPI is monthly, so a daily real change is noise.
 */
public record WatchlistEntryResponse(
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
