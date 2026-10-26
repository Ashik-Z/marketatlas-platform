package com.marketatlas.market.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

/** Raw FRED series/observations JSON. Values arrive as strings; missing values are ".". */
@JsonIgnoreProperties(ignoreUnknown = true)
public record FredObservationsResponse(
        List<Observation> observations,
        @JsonProperty("error_code") Integer errorCode,
        @JsonProperty("error_message") String errorMessage
) {

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Observation(String date, String value) {
    }
}