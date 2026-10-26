package com.marketatlas.market.dto;

import com.marketatlas.enums.CopyrightClass;
import com.marketatlas.enums.SeriesFrequency;
import com.marketatlas.enums.SeriesProvider;
import com.marketatlas.market.entity.DataSeries;

public record SeriesResponse(
        Long id,
        SeriesProvider provider,
        String externalId,
        String name,
        SeriesFrequency frequency,
        String units,
        String sourceOrganization,
        CopyrightClass copyrightClass,
        String attribution
) {

    public static SeriesResponse from(DataSeries series) {
        return new SeriesResponse(
                series.getId(),
                series.getProvider(),
                series.getExternalId(),
                series.getName(),
                series.getFrequency(),
                series.getUnits(),
                series.getSourceOrganization(),
                series.getCopyrightClass(),
                series.getAttribution()
        );
    }
}