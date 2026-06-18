-- Compliance domain security fixes:
-- 1. Add merchant_id to compliance_reminders for data-level tenant isolation.
-- 2. Add unique constraint to prevent duplicate reminders per source + type + due date
--    across repeated scans with different idempotency keys.
ALTER TABLE compliance_reminders
    ADD COLUMN IF NOT EXISTS merchant_id VARCHAR(80);

CREATE INDEX IF NOT EXISTS idx_compliance_reminders_merchant
    ON compliance_reminders (merchant_id)
 WHERE merchant_id IS NOT NULL;

ALTER TABLE compliance_reminders
    ADD CONSTRAINT uq_compliance_reminder_dedup
    UNIQUE (source_type, source_id, reminder_type, due_date);
