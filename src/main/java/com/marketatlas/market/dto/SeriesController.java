package com.marketatlas.market.controller;

import com.marketatlas.market.dto.SeriesObservationDTO;
import com.marketatlas.market.dto.SeriesResponse;
import com.marketatlas.market.service.SeriesService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/series")
public class SeriesController {

    private final SeriesService seriesService;

    public SeriesController(SeriesService seriesService) {
        this.seriesService = seriesService;
    }

    @GetMapping
    public List<SeriesResponse> listSeries() {
        return seriesService.listSeries().stream().map(SeriesResponse::from).toList();
    }

    @GetMapping("/{id}/observations")
    public List<SeriesObservationDTO> getObservations(
            @PathVariable Long id,
            @RequestParam LocalDate startDate,
            @RequestParam LocalDate endDate) {
        return seriesService.getObservations(id, startDate, endDate);
    }

    @PostMapping("/{id}/refresh")
    public Map<String, Integer> refresh(
            @PathVariable Long id,
            @RequestParam LocalDate startDate,
            @RequestParam LocalDate endDate) {
        return Map.of("written", seriesService.refresh(id, startDate, endDate));
    }
}