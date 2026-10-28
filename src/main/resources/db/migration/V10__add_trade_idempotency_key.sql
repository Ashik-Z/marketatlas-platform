ALTER TABLE trade ADD COLUMN idempotency_key VARCHAR(100);
UPDATE trade SET idempotency_key = 'legacy-' || id;
ALTER TABLE trade ALTER COLUMN idempotency_key SET NOT NULL;
ALTER TABLE trade
    ADD CONSTRAINT trade_account_idempotency_key UNIQUE (account_id, idempotency_key);