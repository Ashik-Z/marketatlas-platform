package com.marketatlas.market.controller;

import com.marketatlas.market.service.MarketDataService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import com.marketatlas.market.dto.MarketDataHistoryResponseDTO;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/market-data")
public class MarketDataController {

    @GetMapping("/history")
    public List<MarketDataHistoryResponseDTO> getMarketHistory(
        @RequestParam String symbol, 
        @RequestParam LocalDate startDate, 
        @RequestParam LocalDate endDate, 
        @RequestParam String interval
    ) {
        return marketDataService.fetchHistoricalData(symbol, startDate, endDate, interval);
    }

    private final MarketDataService marketDataService;

    public MarketDataController(MarketDataService marketDataService) {
        this.marketDataService = marketDataService;
    }
}