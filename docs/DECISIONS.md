# MarketAtlas: scope and decisions

A short record of what this project is, what it deliberately is not, and why.

## Scope

- A Spring Boot market-data platform for equities and commodities, with historical prices in PostgreSQL.
- Inflation and commodity-price tracking alongside prices.
- A paper-trading simulation (no real orders, ever) that shows how real-world difficulties are handled.
- Frontend is not a priority. README and OpenAPI docs are written last, once the API is stable.

## Data sources

- **Twelve Data**: equity and spot-metal bars (gold, silver). The API key is sent in the `Authorization` header, never in the URL.
- **FRED**: inflation and energy series (CPIAUCSL, CPIAUCNS, DCOILBRENTEU, DHHNGSP), stored in a provider-agnostic series model (`data_series`, `series_observation`).
- Coal is skipped: a weak inflation signal and licensing friction for the usual sources.
- WTI crude on Twelve Data needs a paid plan, so oil comes from FRED (Brent) instead.
- Series data is cited: each series stores its source organization, copyright class and attribution text.

## Data handling

- History is served from the database first; `market_data_coverage` records which ranges are already stored, so providers are called only for gaps.
- Writes are idempotent batch upserts (`ON CONFLICT DO UPDATE`), one statement per batch.
- Provider calls never run inside a database transaction.
- Missing provider values are stored as NULL, never as zero (FRED's "." marker).
- Provider data lags (monthly CPI, daily oil): series are refreshed with a lookback window, and monthly series use a longer one.
- Vintages (historical revisions) are out of scope: the latest value overwrites and `fetched_at` records when it was fetched.

## Operations and security

- Scheduled refresh jobs are off by default (`marketatlas.refresh.enabled`).
- Schema changes go through Flyway only; tests run with `ddl-auto=validate` so entity/migration drift fails the build.
- All non-GET requests require an admin login (HTTP Basic, stateless). The password comes from the environment and the app will not start without it.
- Secrets live in the untracked `.env`; log messages never include provider error causes, because they can contain request URLs with keys.
- CI runs the full suite (Testcontainers PostgreSQL) on every pull request.

## Known limits and planned work

- Single admin user only; per-user accounts come with the trading simulation.
- No retry or backoff on provider rate limits (HTTP 429) yet.
- Lists are not paginated yet.
- Whether AI features are part of the core is undecided.