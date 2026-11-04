package com.marketatlas.common.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.responses.ApiResponse;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springdoc.core.customizers.OpenApiCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;
import java.util.Map;
import java.util.Set;

@Configuration
public class OpenApiConfig {

    public static final String BASIC_AUTH = "basicAuth";

    private static final Set<String> PUBLIC = Set.of(
            "POST /api/users",
            "GET /api/assets", "GET /api/assets/{id}",
            "GET /api/issuers", "GET /api/issuers/{id}",
            "GET /api/series", "GET /api/series/{id}/observations",
            "GET /api/assets/{assetId}/real-prices", "GET /api/assets/{assetId}/dca");

    private static final Map<String, String> SUMMARIES = Map.ofEntries(
            Map.entry("POST /api/users", "Sign up (always creates a USER)"),
            Map.entry("GET /api/assets", "List assets"),
            Map.entry("GET /api/assets/{id}", "Get an asset"),
            Map.entry("POST /api/assets", "Create an asset (admin)"),
            Map.entry("GET /api/issuers", "List issuers"),
            Map.entry("GET /api/issuers/{id}", "Get an issuer"),
            Map.entry("POST /api/issuers", "Create an issuer (admin)"),
            Map.entry("GET /api/market-data/history", "Fetch and store daily history (admin; can call Twelve Data)"),
            Map.entry("GET /api/assets/{assetId}/real-prices", "Inflation-adjusted prices in base-month dollars (US CPI, USD assets)"),
            Map.entry("GET /api/assets/{assetId}/dca", "Dollar-cost averaging: a fixed amount bought monthly, valued in nominal and real (base-month) dollars (US CPI, USD assets)"),
            Map.entry("POST /api/assets/{assetId}/backtest", "Backtest a strategy on stored daily bars, with a buy-and-hold benchmark (login required)"),
            Map.entry("GET /api/series", "List inflation and commodity series"),
            Map.entry("GET /api/series/{id}/observations", "Series observations in a date range"),
            Map.entry("POST /api/series/{id}/refresh", "Refresh a series from FRED (admin)"),
            Map.entry("POST /api/accounts", "Create a paper-trading account (owner is the caller)"),
            Map.entry("GET /api/accounts", "List your accounts (admin: all accounts)"),
            Map.entry("GET /api/accounts/{id}", "Get an account (404 if it is not yours)"),
            Map.entry("POST /api/accounts/{accountId}/orders",
                    "Place a market order (Idempotency-Key required; 201 new, 200 replay of the same key)"),
            Map.entry("GET /api/accounts/{accountId}/portfolio", "Portfolio valuation with realized and unrealized P&L"),
            Map.entry("GET /api/accounts/{accountId}/trades", "Trade history, newest first, paginated"),
            Map.entry("GET /api/watchlist", "List your watched assets with latest close and nominal day change"),
            Map.entry("PUT /api/watchlist/{assetId}", "Add an asset to your watchlist (idempotent; 50 assets maximum)"),
            Map.entry("DELETE /api/watchlist/{assetId}", "Remove an asset from your watchlist (idempotent)"));

    @Bean
    OpenAPI marketAtlasOpenApi() {
        return new OpenAPI()
                .info(new Info()
                        .title("MarketAtlas API")
                        .version("1.0")
                        .description("Market data for equities and commodities, inflation series, "
                                + "inflation-adjusted prices, and a paper-trading simulation (no real orders). "
                                + "Reads of market data and series are public and served from the database; "
                                + "writes, provider-backed calls and trading need HTTP Basic credentials."))
                .components(new Components().addSecuritySchemes(BASIC_AUTH,
                        new SecurityScheme().type(SecurityScheme.Type.HTTP).scheme("basic")));
    }

    @Bean
    OpenApiCustomizer operationDocs() {
        return openApi -> openApi.getPaths().forEach((path, item) ->
                item.readOperationsMap().forEach((method, operation) -> {
                    String key = method.name() + " " + path;
                    String summary = SUMMARIES.get(key);
                    if (summary != null) {
                        operation.setSummary(summary);
                    }
                    operation.setTags(List.of(tagFor(path)));
                    if (!PUBLIC.contains(key)) {
                        operation.addSecurityItem(new SecurityRequirement().addList(BASIC_AUTH));
                        if (operation.getResponses() != null) {
                            operation.getResponses().addApiResponse("401",
                                    new ApiResponse().description("Missing or wrong credentials"));
                        }
                    }
                    if (key.equals("PUT /api/watchlist/{assetId}") && operation.getResponses() != null) {
                        operation.getResponses().addApiResponse("404", new ApiResponse().description("Asset not found"));
                        operation.getResponses().addApiResponse("409", new ApiResponse().description("Watchlist is full (50 assets)"));
                    }
                    if (key.equals("POST /api/assets/{assetId}/backtest") && operation.getResponses() != null) {
                        operation.getResponses().addApiResponse("404", new ApiResponse().description("Asset not found"));
                        operation.getResponses().addApiResponse("429", new ApiResponse().description("Backtest rate limit exceeded; see Retry-After"));
                    }
                    if (key.equals("POST /api/accounts/{accountId}/orders") && operation.getResponses() != null) {
                        operation.getResponses().addApiResponse("404",
                                new ApiResponse().description("Account not found, or not yours"));
                        operation.getResponses().addApiResponse("409",
                                new ApiResponse().description("Order rejected: price missing or stale, currency mismatch, "
                                        + "insufficient cash or position"));
                        operation.getResponses().addApiResponse("429",
                                new ApiResponse().description("Order rate limit exceeded; see Retry-After"));
                    }
                }));
    }

    private static String tagFor(String path) {
        if (path.startsWith("/api/accounts")) return "Paper trading";
        if (path.startsWith("/api/watchlist")) return "Watchlist";
        if (path.startsWith("/api/market-data")) return "Market data";
        if (path.endsWith("/real-prices") || path.endsWith("/dca")) return "Inflation";
        if (path.endsWith("/backtest")) return "Backtesting";
        if (path.startsWith("/api/series")) return "Series";
        if (path.startsWith("/api/users")) return "Users";
        return "Reference data";
    }
}