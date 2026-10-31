package com.marketatlas.ui.api;

import java.math.BigDecimal;

/** One trading account as returned by GET /api/accounts. */
public record AccountSummary(Long id, String name, String currency, BigDecimal startingBalance, BigDecimal cashBalance) {
}