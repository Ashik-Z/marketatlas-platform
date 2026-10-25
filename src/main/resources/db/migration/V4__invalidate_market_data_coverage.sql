-- Coverage rows recorded before the provider end-date fix assumed the requested end date
-- had been fetched. Twelve Data treats end_date as exclusive for daily bars, so the last
-- day of every recorded range may be missing. Stored bars are kept; coverage is cleared so
-- ranges are re-fetched on demand (writes are idempotent upserts).
DELETE FROM market_data_coverage;