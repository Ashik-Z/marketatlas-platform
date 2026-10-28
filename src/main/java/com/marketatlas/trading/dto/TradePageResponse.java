package com.marketatlas.trading.dto;

import java.util.List;

public record TradePageResponse(
        List<TradeResponse> content,
        int page,
        int size,
        long totalElements,
        int totalPages) {
}