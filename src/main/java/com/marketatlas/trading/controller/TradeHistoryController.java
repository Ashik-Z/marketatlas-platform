package com.marketatlas.trading.controller;

import com.marketatlas.trading.dto.TradePageResponse;
import com.marketatlas.trading.service.TradeHistoryService;
import com.marketatlas.trading.service.AccountService;
import com.marketatlas.trading.service.Caller;
import org.springframework.security.core.Authentication;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/accounts/{accountId}/trades")
public class TradeHistoryController {

    private final TradeHistoryService tradeHistoryService;
    private final AccountService accountService;

    public TradeHistoryController(TradeHistoryService tradeHistoryService, AccountService accountService) {
        this.tradeHistoryService = tradeHistoryService;
        this.accountService = accountService;
    }

    @GetMapping
    public TradePageResponse list(
            @PathVariable Long accountId,
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "50") @Min(1) @Max(200) int size,
            Authentication authentication) {
        accountService.get(accountId, Caller.from(authentication));
        return tradeHistoryService.list(accountId, page, size);
    }
}