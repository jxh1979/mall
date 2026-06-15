-- Security hardening for external credentials and webhook signatures.
-- Legacy plaintext customer sessions cannot be safely backfilled without trusting stored secrets,
-- so they are revoked and callers must log in again after deployment.
ALTER TABLE customer_sessions
    ADD COLUMN IF NOT EXISTS customer_token_hash VARCHAR(64);

UPDATE customer_sessions
   SET status = 'revoked'
 WHERE customer_token_hash IS NULL;

UPDATE customer_sessions
   SET customer_token_hash = repeat('0', 64)
 WHERE customer_token_hash IS NULL;

ALTER TABLE customer_sessions
    ALTER COLUMN customer_token DROP NOT NULL;

UPDATE customer_sessions
   SET customer_token = NULL;

ALTER TABLE customer_sessions
    ALTER COLUMN customer_token_hash SET NOT NULL;

ALTER TABLE customer_sessions
    DROP CONSTRAINT IF EXISTS customer_sessions_customer_token_key;

DROP INDEX IF EXISTS idx_customer_sessions_token_status;

CREATE UNIQUE INDEX IF NOT EXISTS uq_customer_sessions_active_token_hash
    ON customer_sessions (customer_token_hash)
    WHERE status = 'active';

CREATE INDEX IF NOT EXISTS idx_customer_sessions_hash_status
    ON customer_sessions (customer_token_hash, status);

-- Ticket QR credentials already had a hash column; from this migration onward raw QR payloads are not persisted.
-- Rows with non-SHA-256 legacy hashes are revoked because they cannot be verified after plaintext removal.
UPDATE ticket_credentials
   SET status = 'revoked'
 WHERE credential_token IS NOT NULL
   AND credential_hash !~ '^[0-9a-f]{64}$';

ALTER TABLE ticket_credentials
    ALTER COLUMN credential_token DROP NOT NULL;

UPDATE ticket_credentials
   SET credential_token = NULL;

ALTER TABLE ticket_credentials
    DROP CONSTRAINT IF EXISTS ticket_credentials_credential_token_key;

DROP INDEX IF EXISTS idx_ticket_credentials_token_status;

CREATE UNIQUE INDEX IF NOT EXISTS uq_ticket_credentials_active_hash
    ON ticket_credentials (credential_hash)
    WHERE status = 'active';

CREATE INDEX IF NOT EXISTS idx_ticket_credentials_hash_status
    ON ticket_credentials (credential_hash, status);
