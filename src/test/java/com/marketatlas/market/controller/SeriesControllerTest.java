package com.marketatlas.market.controller;

import com.marketatlas.common.exception.MarketDataProviderException;
import com.marketatlas.common.exception.ResourceNotFoundException;
import com.marketatlas.enums.CopyrightClass;
import com.marketatlas.enums.SeriesFrequency;
import com.marketatlas.enums.SeriesProvider;
import com.marketatlas.market.dto.SeriesObservationDTO;
import com.marketatlas.market.entity.DataSeries;
import com.marketatlas.market.service.SeriesService;
import com.marketatlas.market.controller.SeriesController;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(SeriesController.class)
class SeriesControllerTest {

    private static final LocalDate START = LocalDate.of(2026, 9, 1);
    private static final LocalDate END = LocalDate.of(2026, 9, 3);

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private SeriesService seriesService;

    @Test
    void shouldListSeriesWithLicensingMetadata() throws Exception {
        when(seriesService.listSeries()).thenReturn(List.of(new DataSeries(
                SeriesProvider.FRED, "DHHNGSP", "Henry Hub Natural Gas Spot Price",
                SeriesFrequency.DAILY, "US dollars per million BTU", "U.S. Energy Information Administration",
                CopyrightClass.PUBLIC_DOMAIN_CITATION_REQUESTED, "Source: EIA, retrieved from FRED")));

        mockMvc.perform(get("/api/series"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].externalId").value("DHHNGSP"))
                .andExpect(jsonPath("$[0].frequency").value("DAILY"))
                .andExpect(jsonPath("$[0].copyrightClass").value("PUBLIC_DOMAIN_CITATION_REQUESTED"))
                .andExpect(jsonPath("$[0].attribution").value("Source: EIA, retrieved from FRED"));
    }

    @Test
    void shouldReturnObservationsWithMissingValueAsNull() throws Exception {
        when(seriesService.getObservations(5L, START, END)).thenReturn(List.of(
                new SeriesObservationDTO(START, new BigDecimal("3.0")),
                new SeriesObservationDTO(START.plusDays(1), null)));

        mockMvc.perform(get("/api/series/5/observations")
                        .param("startDate", "2026-09-01").param("endDate", "2026-09-03"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].date").value("2026-09-01"))
                .andExpect(jsonPath("$[0].value").value(3.0))
                .andExpect(jsonPath("$[1].value").doesNotExist());
    }

    @Test
    void shouldRefreshAndReportRowsWritten() throws Exception {
        when(seriesService.refresh(5L, START, END)).thenReturn(3);

        mockMvc.perform(post("/api/series/5/refresh")
                        .param("startDate", "2026-09-01").param("endDate", "2026-09-03"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.written").value(3));
    }

    @Test
    void shouldRejectMissingDateParameter() throws Exception {
        mockMvc.perform(get("/api/series/5/observations").param("startDate", "2026-09-01"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void shouldMapUnknownSeriesTo404() throws Exception {
        when(seriesService.getObservations(99L, START, END))
                .thenThrow(new ResourceNotFoundException("Series 99 was not found."));

        mockMvc.perform(get("/api/series/99/observations")
                        .param("startDate", "2026-09-01").param("endDate", "2026-09-03"))
                .andExpect(status().isNotFound());
    }

    @Test
    void shouldMapInvalidRangeTo400() throws Exception {
        when(seriesService.refresh(5L, END, START))
                .thenThrow(new IllegalArgumentException("Start date must not be after end date."));

        mockMvc.perform(post("/api/series/5/refresh")
                        .param("startDate", "2026-09-03").param("endDate", "2026-09-01"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void shouldMapProviderFailureTo502() throws Exception {
        when(seriesService.refresh(5L, START, END))
                .thenThrow(new MarketDataProviderException("FRED rate limit exceeded (HTTP 429)."));

        mockMvc.perform(post("/api/series/5/refresh")
                        .param("startDate", "2026-09-01").param("endDate", "2026-09-03"))
                .andExpect(status().isBadGateway());
    }
}