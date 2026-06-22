ALTER TABLE access_users
    ADD COLUMN IF NOT EXISTS phone_hash VARCHAR(64);

ALTER TABLE access_users
    ADD COLUMN IF NOT EXISTS created_via VARCHAR(64) NOT NULL DEFAULT 'legacy';

ALTER TABLE access_users
    ADD COLUMN IF NOT EXISTS consent_reference VARCHAR(128);

ALTER TABLE access_users
    ADD COLUMN IF NOT EXISTS evidence_attachment_ids TEXT;

ALTER TABLE access_users
    ADD COLUMN IF NOT EXISTS operator_note TEXT;

CREATE INDEX IF NOT EXISTS idx_access_users_org_merchant
    ON access_users (organization_id, merchant_id);
