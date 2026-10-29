ALTER TABLE account ADD COLUMN owner_username VARCHAR(50);
UPDATE account SET owner_username = 'admin' WHERE owner_username IS NULL;
ALTER TABLE account ALTER COLUMN owner_username SET NOT NULL;
CREATE INDEX account_owner_username_idx ON account (owner_username);