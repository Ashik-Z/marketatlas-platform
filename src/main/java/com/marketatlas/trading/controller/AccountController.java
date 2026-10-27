package com.marketatlas.trading.controller;

import com.marketatlas.trading.dto.AccountResponse;
import com.marketatlas.trading.dto.CreateAccountRequest;
import com.marketatlas.trading.service.AccountService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/accounts")
public class AccountController {

    private final AccountService accountService;

    public AccountController(AccountService accountService) {
        this.accountService = accountService;
    }

    @PostMapping
    public ResponseEntity<AccountResponse> create(@Valid @RequestBody CreateAccountRequest request) {
        var account = accountService.create(request.name(), request.currency(), request.startingBalance());
        return ResponseEntity.status(HttpStatus.CREATED).body(AccountResponse.from(account));
    }

    @GetMapping
    public List<AccountResponse> list() {
        return accountService.list().stream().map(AccountResponse::from).toList();
    }

    @GetMapping("/{id}")
    public AccountResponse get(@PathVariable Long id) {
        return AccountResponse.from(accountService.get(id));
    }
}