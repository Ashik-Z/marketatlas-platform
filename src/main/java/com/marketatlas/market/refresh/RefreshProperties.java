package com.marketatlas.market.refresh;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;
import org.springframework.validation.annotation.Validated;

import java.time.Duration;

@Validated
@ConfigurationProperties(prefix = "marketatlas.refresh")
public record RefreshProperties(
        @DefaultValue("false") boolean enabled,
        @DefaultValue("0 30 22 * * MON-FRI") @NotBlank String cron,
        @DefaultValue("7") @Min(1) @Max(30) int lookbackDays,
        @DefaultValue("8s") @NotNull Duration delayBetweenAssets
) {

    public RefreshProperties {
        if (delayBetweenAssets != null && delayBetweenAssets.isNegative()) {
            throw new IllegalArgumentException(
                    "marketatlas.refresh.delay-between-assets must not be negative");
        }
    }
}