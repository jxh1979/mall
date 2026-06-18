-- Evidence domain: data-level tenant isolation.
-- All columns nullable for backward compatibility; populated from the caller's
-- security token at creation time (never from a request parameter).

ALTER TABLE audit_events
    ADD COLUMN IF NOT EXISTS merchant_id VARCHAR(64);

ALTER TABLE evidence_events
    ADD COLUMN IF NOT EXISTS merchant_id VARCHAR(64);

ALTER TABLE evidence_attachments
    ADD COLUMN IF NOT EXISTS merchant_id VARCHAR(64);

ALTER TABLE evidence_reviews
    ADD COLUMN IF NOT EXISTS merchant_id VARCHAR(64);

CREATE INDEX IF NOT EXISTS idx_audit_events_merchant
    ON audit_events (merchant_id)
 WHERE merchant_id IS NOT NULL;

CREATE INDEX IF NOT EXISTS idx_evidence_events_merchant
    ON evidence_events (merchant_id)
 WHERE merchant_id IS NOT NULL;

CREATE INDEX IF NOT EXISTS idx_evidence_attachments_merchant
    ON evidence_attachments (merchant_id)
 WHERE merchant_id IS NOT NULL;

CREATE INDEX IF NOT EXISTS idx_evidence_reviews_merchant
    ON evidence_reviews (merchant_id)
 WHERE merchant_id IS NOT NULL;
