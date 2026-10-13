package com.marketatlas.common.exception;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;

import static org.junit.jupiter.api.Assertions.assertEquals;

class GlobalExceptionHandlerTest {

    private final GlobalExceptionHandler handler =
            new GlobalExceptionHandler();

    @Test
    void shouldReturnBadGatewayForMarketDataProviderException() {
        MarketDataProviderException exception =
                new MarketDataProviderException(
                        "Twelve Data request failed. Code: 400, message: Invalid symbol"
                );

        ProblemDetail response =
                handler.handleMarketDataProviderException(exception);

        assertEquals(HttpStatus.BAD_GATEWAY.value(), response.getStatus());
        assertEquals(
                "Market Data Provider Error",
                response.getTitle()
        );
        assertEquals(
                "The market data provider could not fulfill the request.",
                response.getDetail()
        );
    }
}