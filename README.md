# MarketAtlas

A Spring Boot market-data platform for tracking equities and commodities, integrating external market-data APIs, and persisting historical price data with PostgreSQL. It also tracks inflation, shows prices in inflation-adjusted terms, and includes a paper-trading simulation.

> Paper trading only: no real orders are ever placed, and nothing here is financial advice.

## What it does

- **Market data**: daily bars for equities and spot metals from Twelve Data, stored in PostgreSQL. History is served from the database; a coverage table records which date ranges are already stored, so the provider is only called for gaps.
- **Inflation and commodity series**: CPI and energy series from FRED, stored with their source and attribution.
- **Inflation-adjusted prices**: closes expressed in constant "base month" dollars using US CPI, with nominal vs real return. Months without a published CPI value are flagged rather than guessed.
- **Paper trading**: per-user accounts, market orders filled at the latest stored close, idempotent order placement, average-cost positions, realized/unrealized P&L, and paginated trade history. It deliberately shows real-world difficulties (stale prices, currency mismatch, concurrent orders) and how they are handled.
- **Security**: HTTP Basic with BCrypt, USER and ADMIN roles, per-user account ownership, and rate limits on sign-up, failed logins and orders.

## Stack

Java 25, Spring Boot 4.1.1, PostgreSQL 18, Flyway, Spring Data JPA, Spring Security, Testcontainers, Docker Compose, GitHub Actions CI.

## Quick start

You need Docker, a [Twelve Data](https://twelvedata.com) API key and a [FRED](https://fred.stlouisfed.org/docs/api/api_key.html) API key.

```
# PowerShell                          # bash
Copy-Item .env.example .env           cp .env.example .env
```

Edit `.env` and fill in the four values (it is git-ignored; never commit it), then:

```
docker compose up -d --build
curl http://localhost:8080/api/assets
```

The app listens on port 8080 and PostgreSQL on 5432. It refuses to start if `ADMIN_PASSWORD` is blank. The admin user is `admin` with that password; scheduled refresh jobs are off by default (`marketatlas.refresh.enabled`).

## Running the tests

Docker must be running (tests use Testcontainers with PostgreSQL).

```
./mvnw test          # Windows: .\mvnw.cmd test
```

## API overview

| Endpoint | Access | Purpose |
|---|---|---|
| `POST /api/users` | public | Sign up (always creates a USER) |
| `GET /api/assets`, `/api/assets/{id}` | public | Assets |
| `GET /api/issuers`, `/api/issuers/{id}` | public | Issuers |
| `GET /api/series`, `/api/series/{id}/observations` | public | Inflation and commodity series |
| `GET /api/assets/{id}/real-prices` | public | Inflation-adjusted prices (`startDate`, `endDate`, optional `baseMonth=yyyy-MM`) |
| `POST /api/assets`, `/api/issuers` | admin | Create assets and issuers |
| `GET /api/market-data/history` | admin | Fetch and store history (can call Twelve Data) |
| `POST /api/series/{id}/refresh` | admin | Refresh a series from FRED |
| `POST/GET /api/accounts`, `GET /api/accounts/{id}` | logged-in user | Own accounts only (admin sees all) |
| `POST /api/accounts/{id}/orders` | logged-in user | Place a market order (`Idempotency-Key` header required) |
| `GET /api/accounts/{id}/portfolio`, `/trades` | logged-in user | Valuation with P&L; paginated trade history |
| `POST /api/assets/{id}/backtest` | logged-in user | Backtest a strategy against buy-and-hold, in nominal and real terms |

Public reads come from the database only. Another user's account answers 404, the same as a missing one.

Interactive API docs (Swagger UI) are at `http://localhost:8080/swagger-ui.html` and the OpenAPI spec at `/v3/api-docs`; both are public.

### Example

```
curl -X POST http://localhost:8080/api/users -H "Content-Type: application/json" \
  -d '{"username":"alice","password":"a-long-password-123"}'

curl -u alice:a-long-password-123 -X POST http://localhost:8080/api/accounts \
  -H "Content-Type: application/json" \
  -d '{"name":"Practice","currency":"USD","startingBalance":10000}'

curl -u alice:a-long-password-123 -X POST http://localhost:8080/api/accounts/1/orders \
  -H "Content-Type: application/json" -H "Idempotency-Key: my-first-order" \
  -d '{"assetId":2,"side":"BUY","quantity":1}'
```

On Windows PowerShell use `curl.exe`, since `curl` is an alias for another command there. Passwords need at least 12 characters.

## Backtesting

`POST /api/assets/{id}/backtest` replays a strategy on the stored daily bars (no provider calls) and always compares it with buy-and-hold under identical costs. It needs a login and is limited to 10 runs per minute per user.

```json
{ "startDate": "2024-01-01", "endDate": "2026-10-30", "strategy": "SMA_CROSSOVER",
  "shortWindow": 20, "longWindow": 50, "initialCash": 100000, "slippageBps": 5 }
```

Gold Spot, 2024-01 to 2026-10, $1 commission per order, 5 bps slippage:

| | SMA 20/50 | Buy and hold |
|---|---|---|
| Total return | 74.22% | 97.20% |
| Real return (start-date dollars) | 59.48% | 80.51% |
| Max drawdown | 21.06% | 26.43% |
| Annualized volatility | 18.46% | 21.23% |
| Time in market | 68.07% | 99.86% |
| Trades | 12 | 1 |

US CPI rose 9.25% over the period, so both beat inflation; the trend rule gave up return for a shallower drawdown. One asset, one period: an illustration, not a recommendation. Rules and limits are in `docs/DECISIONS.md`.

## Web UI

`marketatlas-ui/` is a separate Vaadin 25 app (port 8081) that talks to the API; the API works on its own without it. `docker compose up -d` starts the database, the API (8080) and the UI (8081). Open http://localhost:8081, sign up or log in. The UI keeps your credentials only in its server-side session (never in the browser), and logout ends that session.

The UI pages:

- **Markets** (`/market`) lists the assets; a row opens the asset page with the nominal vs inflation-adjusted price chart (USD assets only).
- **Backtest lab** (`/backtest`) runs the backtester and shows strategy vs buy-and-hold metrics (nominal and real), the equity curves and the trades.
- **Accounts** (`/accounts`, `/account/{id}`) create paper-trading accounts, show the portfolio with realized and unrealized P&L, place market orders and list the trade history.

For UI development, run `.\mvnw.cmd spring-boot:run` inside `marketatlas-ui/` while the API is up on port 8080.

## Design notes

Decisions, trade-offs and known limits are written down in [docs/DECISIONS.md](docs/DECISIONS.md): data handling, the security model, rate limits, and how provider quirks (weekend bars, rate limits, missing CPI months) are handled.

## Data sources and attribution

Market data: Twelve Data. Inflation and commodity series: FRED, Federal Reserve Bank of St. Louis. Each series stores its source organization and attribution text, and the API returns it.