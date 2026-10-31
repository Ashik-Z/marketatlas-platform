package com.marketatlas.ui.api;

/** One row of GET /api/assets (only the fields the UI uses). */
public record AssetSummary(Long id, String name, String assetClass, String symbol, String currency, String unit) {
}