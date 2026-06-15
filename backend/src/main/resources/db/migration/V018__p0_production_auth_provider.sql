CREATE TABLE IF NOT EXISTS auth_credentials (
    user_id VARCHAR(64) PRIMARY KEY REFERENCES access_users(id) ON DELETE CASCADE,
    password_hash VARCHAR(255) NOT NULL,
    password_algo VARCHAR(64) NOT NULL DEFAULT 'bcrypt',
    failed_attempt_count INTEGER NOT NULL DEFAULT 0,
    locked_until TIMESTAMPTZ,
    last_login_at TIMESTAMPTZ,
    password_changed_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    status VARCHAR(32) NOT NULL DEFAULT 'active',
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT chk_auth_credentials_status CHECK (status IN ('active', 'disabled', 'locked'))
);

CREATE TABLE IF NOT EXISTS auth_sessions (
    id VARCHAR(64) PRIMARY KEY,
    user_id VARCHAR(64) NOT NULL REFERENCES access_users(id) ON DELETE CASCADE,
    access_token_hash VARCHAR(64) NOT NULL,
    refresh_token_hash VARCHAR(64) NOT NULL,
    device_id VARCHAR(128) NOT NULL DEFAULT 'unknown',
    terminal_type VARCHAR(64) NOT NULL DEFAULT 'web',
    status VARCHAR(32) NOT NULL DEFAULT 'active',
    access_expires_at TIMESTAMPTZ NOT NULL,
    refresh_expires_at TIMESTAMPTZ NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    revoked_at TIMESTAMPTZ,
    CONSTRAINT chk_auth_sessions_status CHECK (status IN ('active', 'logged_out', 'expired', 'revoked'))
);

CREATE UNIQUE INDEX IF NOT EXISTS uq_auth_sessions_active_access_token_hash
    ON auth_sessions (access_token_hash)
    WHERE status = 'active';

CREATE UNIQUE INDEX IF NOT EXISTS uq_auth_sessions_active_refresh_token_hash
    ON auth_sessions (refresh_token_hash)
    WHERE status = 'active';

CREATE INDEX IF NOT EXISTS idx_auth_sessions_user_status
    ON auth_sessions (user_id, status, created_at DESC);

CREATE INDEX IF NOT EXISTS idx_auth_credentials_locked_until
    ON auth_credentials (locked_until)
    WHERE locked_until IS NOT NULL;
