package com.marketatlas.trading.model;

import java.math.BigDecimal;

public record PositionQuantity(Long assetId, BigDecimal quantity) {
}