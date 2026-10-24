package com.marketatlas.market.dto;

import com.marketatlas.enums.AssetClass;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record CreateAssetRequest(
        @NotBlank @Size(max = 255) String name,
        @NotNull AssetClass assetClass,
        Long issuerId,
        @Size(max = 255) String symbol
) {
}