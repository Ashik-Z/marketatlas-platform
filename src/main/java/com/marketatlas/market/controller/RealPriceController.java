package com.marketatlas.market.controller;

import com.marketatlas.market.dto.RealPriceResponse;
import com.marketatlas.market.service.RealPriceService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.time.YearMonth;

@RestController
@RequestMapping("/api/assets/{assetId}/real-prices")
public class RealPriceController {

    private final RealPriceService realPriceService;

    public RealPriceController(RealPriceService realPriceService) {
        this.realPriceService = realPriceService;
    }

    @GetMapping
    public RealPriceResponse get(
            @PathVariable Long assetId,
            @RequestParam LocalDate startDate,
            @RequestParam LocalDate endDate,
            @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM") YearMonth baseMonth) {
        return realPriceService.getRealPrices(assetId, startDate, endDate, baseMonth);
    }
}