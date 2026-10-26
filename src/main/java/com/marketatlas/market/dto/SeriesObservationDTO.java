package com.marketatlas.market.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

/** One observation; value is null when the provider reports the date as missing. */
public record SeriesObservationDTO(LocalDate date, BigDecimal value) {
}