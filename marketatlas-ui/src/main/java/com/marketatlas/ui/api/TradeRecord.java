package com.marketatlas.ui.api;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

/** One executed trade (order fill). */
public record TradeRecord(Long id, Long accountId, Long assetId, String side, BigDecimal quantity, BigDecimal price,
                          BigDecimal commission, LocalDate priceDate, Instant executedAt) {
}