package com.marketatlas.trading.controller;

import com.marketatlas.trading.dto.PlaceOrderRequest;
import com.marketatlas.trading.dto.TradeResponse;
import com.marketatlas.trading.service.OrderService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/accounts/{accountId}/orders")
public class OrderController {

    private final OrderService orderService;

    public OrderController(OrderService orderService) {
        this.orderService = orderService;
    }

    @PostMapping
    public ResponseEntity<TradeResponse> placeMarketOrder(@PathVariable Long accountId,
                                                          @Valid @RequestBody PlaceOrderRequest request) {
        var trade = orderService.placeMarketOrder(
                accountId, request.assetId(), request.side(), request.quantity());
        return ResponseEntity.status(HttpStatus.CREATED).body(TradeResponse.from(trade));
    }
}