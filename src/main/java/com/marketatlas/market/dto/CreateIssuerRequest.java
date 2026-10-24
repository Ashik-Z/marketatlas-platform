package com.marketatlas.market.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreateIssuerRequest(
        @NotBlank @Size(max = 255) String name,
        @NotBlank @Size(max = 255) String country
) {
}