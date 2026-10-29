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
- Provider bars dated Saturday or Sunday are dropped before saving: equities and spot metals have no full weekend session, and the provider has returned partial weekend rows (once the same Sunday twice). A duplicate on a weekday is still rejected as a provider fault rather than guessed.
- Inflation-adjusted (real) prices (`GET /api/assets/{id}/real-prices`) deflate closes with US CPI-U, not seasonally adjusted (FRED CPIAUCNS), because seasonally adjusted series are revised after publication and would silently change past results. CPI is monthly and lagged: a bar in a month without a CPI value reuses the latest earlier month and is flagged `cpiEstimated`. Live data showed this twice: the newest month, and October 2025, which FRED has no value for, so that month's bars carry September's CPI. Bars before the first CPI month get no real value rather than a guess.
- Provider data lags (monthly CPI, daily oil): series are refreshed with a lookback window, and monthly series use a longer one.
- Vintages (historical revisions) are out of scope: the latest value overwrites and `fetched_at` records when it was fetched.

## Operations and security

- Scheduled refresh jobs are off by default (`marketatlas.refresh.enabled`).
- Schema changes go through Flyway only; tests run with `ddl-auto=validate` so entity/migration drift fails the build.
- CI runs the full suite (Testcontainers PostgreSQL) on every pull request.
- Secrets live in the untracked `.env`; log messages never include provider error causes, because they can contain request URLs with keys.
- **Authentication**: HTTP Basic, stateless, BCrypt. No JWT or sessions: for a single-service API this avoids token storage, signing keys and revocation, which are easy to get wrong. CSRF protection is off because there are no cookies or sessions to ride on.
- **Users**: sign-up (`POST /api/users`) is open and always creates a USER. Passwords need at least 12 characters and at most 72 bytes (BCrypt's input limit). The bootstrap admin comes from `ADMIN_USERNAME` / `ADMIN_PASSWORD` in the environment, is not a database row, has its username reserved at sign-up, and the app will not start without its password.
- **Authorization** (first matching rule wins): sign-up is public; `/api/accounts/**` needs a logged-in user; `/api/market-data/**` and every other non-GET request are admin-only; the remaining GET reads are public.
- Public reads are database-only. Anything that can call a data provider (market-data history, series refresh) needs the admin login.
- **Ownership**: every account has an owner. Users see and trade only their own accounts; another user's account answers 404, identical to a missing one, so account ids cannot be probed. Admin can see all accounts. Accounts that existed before ownership belong to `admin`.
- **Rate limiting** (in memory, per instance): sign-up 5 per hour per IP; failed logins 10 per 15 minutes per IP, and while an IP is locked out even correct credentials from it are refused; orders 30 per minute per user. Blocked calls get 429 with `Retry-After`. The client IP is the socket address only: `X-Forwarded-For` is ignored because clients can forge it. Failures are keyed by IP, not username, so nobody can lock a victim out of their account.
- 401, 403 and 429 responses are `application/problem+json` and echo nothing from the request.
- Provider rate limits (429 from Twelve Data or FRED) surface as 503 with `Retry-After`; refresh jobs stop for that run and never retry in a loop.

## Known limits and planned work

- Rate-limit state is per instance and resets on restart; running several instances would need a shared store. Users behind one shared IP share the sign-up and failed-login budgets. Behind a trusted proxy, the client-IP source must be configured first.
- No password reset, email verification or account deletion; usernames are permanent.
- Only trade history is paginated so far.
- Whether AI features are part of the core is undecided.
- Daily bars only: the bar for the current day is partial while the market is open, and market orders fill at the latest stored close, so a fill today uses an in-progress price.
- Weekend bars already stored before this rule (from earlier fetches) are not removed; they are partial sessions and can be the latest close used for a fill or valuation.
- Real prices cover USD assets only (US CPI); other currencies would need their own inflation series.
