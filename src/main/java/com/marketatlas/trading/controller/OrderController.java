package com.marketatlas.trading.controller;

import com.marketatlas.trading.dto.PlaceOrderRequest;
import com.marketatlas.trading.dto.TradeResponse;
import com.marketatlas.trading.service.OrderService;
import com.marketatlas.trading.service.AccountService;
import com.marketatlas.trading.service.Caller;
import org.springframework.security.core.Authentication;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.RequestHeader;

@RestController
@RequestMapping("/api/accounts/{accountId}/orders")
public class OrderController {

    private final OrderService orderService;
    private final AccountService accountService;

    public OrderController(OrderService orderService, AccountService accountService) {
        this.orderService = orderService;
        this.accountService = accountService;
    }

    @PostMapping
    public ResponseEntity<TradeResponse> placeMarketOrder(
            @PathVariable Long accountId,
            @RequestHeader("Idempotency-Key") String idempotencyKey,
            @Valid @RequestBody PlaceOrderRequest request,
            Authentication authentication) {
        accountService.get(accountId, Caller.from(authentication));
        var result = orderService.placeMarketOrder(
                accountId, request.assetId(), request.side(), request.quantity(), idempotencyKey);
        HttpStatus status = result.replayed() ? HttpStatus.OK : HttpStatus.CREATED;
        return ResponseEntity.status(status).body(TradeResponse.from(result.trade()));
    }
}