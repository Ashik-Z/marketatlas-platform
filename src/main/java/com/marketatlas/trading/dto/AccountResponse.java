package com.marketatlas.trading.dto;

import com.marketatlas.trading.model.Account;

import java.math.BigDecimal;
import java.time.Instant;

public record AccountResponse(
        Long id,
        String name,
        String currency,
        BigDecimal startingBalance,
        BigDecimal cashBalance,
        Instant createdAt) {

    public static AccountResponse from(Account account) {
        return new AccountResponse(account.getId(), account.getName(), account.getCurrency(),
                account.getStartingBalance(), account.getCashBalance(), account.getCreatedAt());
    }
}