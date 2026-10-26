package com.marketatlas.market.client;

import com.marketatlas.market.dto.SeriesObservationDTO;

import java.time.LocalDate;
import java.util.List;

/** A provider of single-value time series. End date is inclusive. */
public interface SeriesClient {

    List<SeriesObservationDTO> getObservations(String externalId, LocalDate startDate, LocalDate endDate);
}