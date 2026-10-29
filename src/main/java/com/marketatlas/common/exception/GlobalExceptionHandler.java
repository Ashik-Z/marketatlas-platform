package com.marketatlas.common.exception;

import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.validation.FieldError;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.web.bind.MissingServletRequestParameterException;

import org.springframework.web.method.annotation.HandlerMethodValidationException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log =
            LoggerFactory.getLogger(GlobalExceptionHandler.class);

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

        log.warn("Market data provider error: {}", exception.getMessage());
    
        ProblemDetail problemDetail =
                ProblemDetail.forStatus(HttpStatus.BAD_GATEWAY);
    
        problemDetail.setTitle("Market Data Provider Error");
        problemDetail.setDetail(
                "The market data provider could not fulfill the request."
        );
    
        return problemDetail;
    }

    @ExceptionHandler(ProviderRateLimitException.class)
    public ResponseEntity<ProblemDetail> handleProviderRateLimit(ProviderRateLimitException exception) {

        log.warn("Market data provider rate limit: {}", exception.getMessage());

        ProblemDetail problemDetail =
                ProblemDetail.forStatus(HttpStatus.SERVICE_UNAVAILABLE);

        problemDetail.setTitle("Market Data Provider Rate Limit");
        problemDetail.setDetail(
                "The market data provider is rate limiting requests. Try again later."
        );

        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                .header(HttpHeaders.RETRY_AFTER, String.valueOf(exception.getRetryAfterSeconds()))
                .body(problemDetail);
    }

    @ExceptionHandler(MissingServletRequestParameterException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ProblemDetail handleMissingServletRequestParameter(
            MissingServletRequestParameterException exception) {

        ProblemDetail problemDetail =
                ProblemDetail.forStatus(HttpStatus.BAD_REQUEST);

        problemDetail.setTitle("Bad Request");
        problemDetail.setDetail(
                "Required request parameter '" + exception.getParameterName()
                        + "' is missing."
        );

        return problemDetail;
    }

    @ExceptionHandler(ResourceNotFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public ProblemDetail handleResourceNotFound(ResourceNotFoundException exception) {

        ProblemDetail problemDetail =
                ProblemDetail.forStatus(HttpStatus.NOT_FOUND);

        problemDetail.setTitle("Not Found");
        problemDetail.setDetail(exception.getMessage());

        return problemDetail;
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ProblemDetail handleBodyValidation(MethodArgumentNotValidException exception) {

        ProblemDetail problemDetail =
                ProblemDetail.forStatus(HttpStatus.BAD_REQUEST);

        problemDetail.setTitle("Bad Request");
        problemDetail.setDetail("The request body is invalid.");
        problemDetail.setProperty(
                "errors",
                exception.getBindingResult().getFieldErrors().stream()
                        .collect(java.util.stream.Collectors.toMap(
                                FieldError::getField,
                                error -> String.valueOf(error.getDefaultMessage()),
                                (first, second) -> first
                        ))
        );

        return problemDetail;
    }

    @ExceptionHandler(DuplicateResourceException.class)
    @ResponseStatus(HttpStatus.CONFLICT)
    public ProblemDetail handleDuplicateResource(DuplicateResourceException exception) {

        ProblemDetail problemDetail =
                ProblemDetail.forStatus(HttpStatus.CONFLICT);

        problemDetail.setTitle("Conflict");
        problemDetail.setDetail(exception.getMessage());

        return problemDetail;
    }

    @ExceptionHandler(OrderRejectedException.class)
    @ResponseStatus(HttpStatus.CONFLICT)
    public ProblemDetail handleOrderRejected(OrderRejectedException exception) {

        ProblemDetail problemDetail =
                ProblemDetail.forStatus(HttpStatus.CONFLICT);

        problemDetail.setTitle("Order Rejected");
        problemDetail.setDetail(exception.getMessage());

        return problemDetail;
    }
}
