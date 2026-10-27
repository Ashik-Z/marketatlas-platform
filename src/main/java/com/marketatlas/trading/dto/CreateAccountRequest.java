package com.marketatlas.trading.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

public record CreateAccountRequest(
        @NotBlank @Size(max = 100) String name,
        @NotNull @Pattern(regexp = "[A-Z]{3}", message = "must be a 3-letter uppercase ISO 4217 code") String currency,
        @NotNull @DecimalMin("0") @Digits(integer = 14, fraction = 6) BigDecimal startingBalance) {
}