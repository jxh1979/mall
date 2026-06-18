-- Add merchant_id to ar_ap_items for data-level tenant isolation.
-- Nullable so existing rows are backward-compatible; populated from the caller's
-- security token at creation time (never from a request parameter).
ALTER TABLE ar_ap_items ADD COLUMN IF NOT EXISTS merchant_id VARCHAR(64);

CREATE INDEX IF NOT EXISTS idx_ar_ap_items_merchant
    ON ar_ap_items (merchant_id)
 WHERE merchant_id IS NOT NULL;

-- Prevent duplicate daily-close locks for the same business date + scope.
-- The idempotency key guard only protects exact retries; this constraint
-- catches different keys targeting the same logical date + scope.
ALTER TABLE finance_daily_closes
    ADD CONSTRAINT uq_daily_close_scope_date UNIQUE (business_date, scope_type, scope_id);
