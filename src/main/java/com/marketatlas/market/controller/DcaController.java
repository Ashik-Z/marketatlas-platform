package com.marketatlas.market.controller;

import com.marketatlas.market.dto.DcaResponse;
import com.marketatlas.market.service.DcaService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;

@RestController
@RequestMapping("/api/assets/{assetId}/dca")
public class DcaController {

    private final DcaService dcaService;

    public DcaController(DcaService dcaService) {
        this.dcaService = dcaService;
    }

    @GetMapping
    public DcaResponse get(
            @PathVariable Long assetId,
            @RequestParam LocalDate startDate,
            @RequestParam LocalDate endDate,
            @RequestParam BigDecimal amount,
            @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM") YearMonth baseMonth) {
        return dcaService.run(assetId, startDate, endDate, amount, baseMonth);
    }
}
