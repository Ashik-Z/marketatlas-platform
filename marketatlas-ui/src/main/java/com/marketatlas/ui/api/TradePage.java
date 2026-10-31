package com.marketatlas.ui.api;

import java.util.List;

/** One page of GET /api/accounts/{id}/trades (newest first). */
public record TradePage(List<TradeRecord> content, int page, int size, long totalElements, int totalPages) {
}