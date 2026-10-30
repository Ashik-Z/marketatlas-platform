package com.marketatlas.ui.api;

import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ProblemDetail;
import org.springframework.stereotype.Component;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Map;

/** Thin client for the MarketAtlas REST API. Never logs or echoes credentials. */
@Component
public class ApiClient {

    private final RestClient rest;

    public ApiClient(RestClient apiRestClient) {
        this.rest = apiRestClient;
    }

    public static String basicHeader(String username, String password) {
        String raw = username + ":" + password;
        return "Basic " + Base64.getEncoder().encodeToString(raw.getBytes(StandardCharsets.UTF_8));
    }

    public void signUp(String username, String password) {
        try {
            rest.post().uri("/api/users")
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(Map.of("username", username, "password", password))
                    .retrieve()
                    .toBodilessEntity();
        } catch (RestClientResponseException e) {
            throw map(e);
        } catch (ResourceAccessException e) {
            throw unavailable(e);
        }
    }

    /** Verifies credentials with a cheap authenticated call; returns normally only on success. */
    public void checkLogin(String username, String password) {
        try {
            rest.get().uri("/api/accounts")
                    .header(HttpHeaders.AUTHORIZATION, basicHeader(username, password))
                    .retrieve()
                    .toBodilessEntity();
        } catch (RestClientResponseException e) {
            throw map(e);
        } catch (ResourceAccessException e) {
            throw unavailable(e);
        }
    }

    private static final org.slf4j.Logger LOG = org.slf4j.LoggerFactory.getLogger(ApiClient.class);

    private static ApiException unavailable(Exception e) {
        Throwable root = e.getCause() != null ? e.getCause() : e;
        LOG.warn("API call failed: {}", root.getClass().getName());
        return new ApiException(ApiException.Kind.UNAVAILABLE, "The MarketAtlas API is not reachable right now.", null);
    }

    private static ApiException map(RestClientResponseException e) {
        int status = e.getStatusCode().value();
        return switch (status) {
            case 400 -> new ApiException(ApiException.Kind.INVALID_INPUT, detailOr(e, "The input was not accepted."), null);
            case 401 -> new ApiException(ApiException.Kind.INVALID_CREDENTIALS, "Wrong username or password.", null);
            case 409 -> new ApiException(ApiException.Kind.DUPLICATE_USER, "That username is not available.", null);
            case 429 -> new ApiException(ApiException.Kind.RATE_LIMITED, "Too many attempts. Please wait and try again.",
                    retryAfter(e));
            default -> new ApiException(ApiException.Kind.UNAVAILABLE, "The MarketAtlas API returned an error (" + status + ").", null);
        };
    }

    private static String detailOr(RestClientResponseException e, String fallback) {
        try {
            ProblemDetail problem = e.getResponseBodyAs(ProblemDetail.class);
            if (problem != null && problem.getDetail() != null && !problem.getDetail().isBlank()) {
                return problem.getDetail();
            }
        } catch (RuntimeException ignored) {
            // fall through to the generic text
        }
        return fallback;
    }

    private static Long retryAfter(RestClientResponseException e) {
        HttpHeaders headers = e.getResponseHeaders();
        String value = headers == null ? null : headers.getFirst(HttpHeaders.RETRY_AFTER);
        if (value == null) {
            return null;
        }
        try {
            return Long.parseLong(value.trim());
        } catch (NumberFormatException ex) {
            return null;
        }
    }
}