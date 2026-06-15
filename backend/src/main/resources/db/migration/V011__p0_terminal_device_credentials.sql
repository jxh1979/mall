CREATE TABLE IF NOT EXISTS terminal_device_credentials (
    device_id VARCHAR(128) PRIMARY KEY REFERENCES terminal_devices(device_id) ON DELETE CASCADE,
    token_sha256 VARCHAR(64) NOT NULL,
    status VARCHAR(32) NOT NULL DEFAULT 'active',
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,
    CONSTRAINT chk_terminal_device_credentials_status CHECK (status IN ('active', 'revoked'))
);

CREATE INDEX IF NOT EXISTS idx_terminal_device_credentials_status
    ON terminal_device_credentials (status, updated_at DESC);
