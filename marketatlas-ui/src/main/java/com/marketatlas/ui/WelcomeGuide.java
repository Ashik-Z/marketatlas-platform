package com.marketatlas.ui;

import java.util.List;

/** The steps shown on the welcome page: one tour for regular users, one for the administrator. */
final class WelcomeGuide {

    record Step(String title, String text, String linkLabel, String href, boolean external) {
    }

    private WelcomeGuide() {
    }

    static List<Step> forUser() {
        return List.of(
                new Step("Browse the markets",
                        "Every asset we track, with its latest price. Star the ones you care about to build "
                                + "your watchlist (up to 50 assets).",
                        "Open Markets", "market", false),
                new Step("Compare nominal and real prices",
                        "Click an asset to see its price twice: nominal, as a broker shows it, and real, "
                                + "adjusted for inflation with US CPI. Pick any dates you like.",
                        "Open Markets", "market", false),
                new Step("See what saving monthly would have done",
                        "Under the chart, the \"If you had bought every month\" panel buys a fixed amount each month "
                                + "and shows the gain before and after inflation.",
                        "Open Markets", "market", false),
                new Step("Test a strategy",
                        "The Backtest lab runs a moving-average crossover or buy-and-hold on stored price history "
                                + "and compares it with buying and holding, in nominal and real terms.",
                        "Open the Backtest lab", "backtest", false),
                new Step("Practise trading with paper money",
                        "Create an account with starting cash, place market orders, and follow your positions, "
                                + "profit and loss, and trade history. It is a simulation: no real money moves.",
                        "Open Accounts", "accounts", false),
                new Step("Keep an eye on everything",
                        "The Overview shows your accounts, latest trades and watchlist at a glance.",
                        "Open the Overview", "", false));
    }

    static List<Step> forAdmin(String docsUrl) {
        return List.of(
                new Step("Open the API docs",
                        "Admin tasks run through the API docs (Swagger UI). Click Authorize, choose basicAuth, "
                                + "and sign in with the admin username and the ADMIN_PASSWORD you configured.",
                        "Open the API docs", docsUrl, true),
                new Step("Register issuers and assets",
                        "POST /api/issuers and POST /api/assets add what users can track. Set the currency and "
                                + "unit: USD assets get real-terms figures, the DCA panel and real-terms backtests.",
                        "Open the API docs", docsUrl, true),
                new Step("Fetch price history",
                        "GET /api/market-data/history (symbol, startDate, endDate) pulls daily bars from "
                                + "Twelve Data and stores them. Each fetch uses your provider quota, which is small "
                                + "on the free plan, so fetch a range once.",
                        "Open the API docs", docsUrl, true),
                new Step("Load inflation data",
                        "POST /api/series/{id}/refresh (startDate, endDate) loads CPI and the other FRED series. "
                                + "Real prices need CPIAUCNS loaded; GET /api/series lists the series and their ids.",
                        "Open the API docs", docsUrl, true),
                new Step("Keep data fresh automatically",
                        "Turn on the scheduled refresh (the marketatlas.refresh setting, see the README) to update "
                                + "stored prices and series by itself. It stops early if a provider rate-limits.",
                        "Open the API docs", docsUrl, true),
                new Step("See what only you see",
                        "The Users page lists everyone who has signed up, and the Accounts page lists every user's paper accounts, while regular users only ever see "
                                + "their own. Paper trading and watchlists are for users, so you can read accounts but not trade.",
                        "Open Users", "users", false));
    }
}


