package com.marketatlas.trading.service;

import com.marketatlas.common.exception.ResourceNotFoundException;
import com.marketatlas.trading.model.Account;
import com.marketatlas.trading.repository.AccountRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Clock;
import java.util.List;

@Service
public class AccountService {

    private final AccountRepository accountRepository;
    private final Clock clock;

    public AccountService(AccountRepository accountRepository, Clock clock) {
        this.accountRepository = accountRepository;
        this.clock = clock;
    }

    @Transactional
    public Account create(String name, String currency, BigDecimal startingBalance) {
        return accountRepository.save(
                new Account(name.trim(), currency, startingBalance, clock.instant()));
    }

    @Transactional(readOnly = true)
    public Account get(Long id) {
        return accountRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Account not found: " + id));
    }

    @Transactional(readOnly = true)
    public List<Account> list() {
        return accountRepository.findAll();
    }
}