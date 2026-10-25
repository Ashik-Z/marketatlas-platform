-- Quote currency (ISO 4217, e.g. USD) and pricing unit (e.g. 'troy ounce') for an asset.
-- Both are nullable: existing assets and providers that do not state them stay valid, and
-- we only record what we actually know rather than guessing.
ALTER TABLE asset ADD COLUMN currency varchar(3);
ALTER TABLE asset ADD COLUMN unit varchar(50);
ALTER TABLE asset ADD CONSTRAINT ck_asset_currency_format
    CHECK (currency IS NULL OR currency ~ '^[A-Z]{3}$');