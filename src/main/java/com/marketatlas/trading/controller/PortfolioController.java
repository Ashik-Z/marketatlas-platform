package com.marketatlas.trading.controller;

import com.marketatlas.trading.dto.PortfolioResponse;
import com.marketatlas.trading.service.PortfolioService;
import com.marketatlas.trading.service.AccountService;
import com.marketatlas.trading.service.Caller;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/accounts/{accountId}/portfolio")
public class PortfolioController {

    private final PortfolioService portfolioService;
    private final AccountService accountService;

    public PortfolioController(PortfolioService portfolioService, AccountService accountService) {
        this.portfolioService = portfolioService;
        this.accountService = accountService;
    }

    @GetMapping
    public PortfolioResponse get(@PathVariable Long accountId, Authentication authentication) {
        accountService.get(accountId, Caller.from(authentication));
        return portfolioService.getPortfolio(accountId);
    }
}