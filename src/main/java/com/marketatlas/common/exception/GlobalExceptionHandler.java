package com.marketatlas.common.exception;

import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import org.springframework.web.method.annotation.HandlerMethodValidationException;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(IllegalArgumentException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ProblemDetail handleIllegalArgumentException(IllegalArgumentException exception) {
        
            ProblemDetail problemDetail = ProblemDetail.forStatus(HttpStatus.BAD_REQUEST);
            problemDetail.setTitle("Bad Request"); 
            problemDetail.setDetail(exception.getMessage()); 
            return problemDetail;
    }

    @ExceptionHandler(HandlerMethodValidationException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ProblemDetail handleMethodValidationException(
            HandlerMethodValidationException exception) {
    
        ProblemDetail problemDetail =
                ProblemDetail.forStatus(HttpStatus.BAD_REQUEST);
    
        problemDetail.setTitle("Bad Request");
        problemDetail.setDetail("One or more request parameters are invalid.");
    
        return problemDetail;
    }

    @ExceptionHandler(MarketDataProviderException.class)
    @ResponseStatus(HttpStatus.BAD_GATEWAY)
    public ProblemDetail handleMarketDataProviderException(
            MarketDataProviderException exception) {
    
        ProblemDetail problemDetail =
                ProblemDetail.forStatus(HttpStatus.BAD_GATEWAY);
    
        problemDetail.setTitle("Market Data Provider Error");
        problemDetail.setDetail(
                "The market data provider could not fulfill the request."
        );
    
        return problemDetail;
    }
}
