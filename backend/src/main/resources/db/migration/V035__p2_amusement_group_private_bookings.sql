ALTER TABLE amusement_sessions
    ADD COLUMN IF NOT EXISTS locked_capacity INTEGER NOT NULL DEFAULT 0;

ALTER TABLE amusement_sessions
    DROP CONSTRAINT IF EXISTS chk_amusement_sessions_used_capacity;

ALTER TABLE amusement_sessions
    DROP CONSTRAINT IF EXISTS chk_amusement_sessions_used_locked_capacity;

ALTER TABLE amusement_sessions
    ADD CONSTRAINT chk_amusement_sessions_used_locked_capacity
    CHECK (used_capacity >= 0 AND locked_capacity >= 0 AND used_capacity + locked_capacity <= capacity);

CREATE TABLE IF NOT EXISTS amusement_group_bookings (
    id VARCHAR(64) PRIMARY KEY,
    project_id VARCHAR(64) NOT NULL REFERENCES amusement_projects(id) ON DELETE CASCADE,
    session_id VARCHAR(64) NOT NULL REFERENCES amusement_sessions(id) ON DELETE CASCADE,
    booking_type VARCHAR(32) NOT NULL,
    customer_name VARCHAR(120) NOT NULL,
    contact_phone VARCHAR(64),
    party_size INTEGER NOT NULL,
    reserved_capacity INTEGER NOT NULL,
    booking_status VARCHAR(32) NOT NULL,
    approval_note TEXT,
    operator_note TEXT,
    evidence_attachment_ids JSONB NOT NULL DEFAULT '[]'::jsonb,
    approved_at TIMESTAMPTZ,
    released_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,
    CONSTRAINT chk_amusement_group_bookings_type CHECK (booking_type IN ('group', 'private')),
    CONSTRAINT chk_amusement_group_bookings_party_size CHECK (party_size > 0),
    CONSTRAINT chk_amusement_group_bookings_reserved_capacity CHECK (reserved_capacity > 0),
    CONSTRAINT chk_amusement_group_bookings_status CHECK (booking_status IN ('pending_approval', 'approved', 'released', 'cancelled')),
    CONSTRAINT chk_amusement_group_bookings_evidence CHECK (jsonb_typeof(evidence_attachment_ids) = 'array')
);

CREATE INDEX IF NOT EXISTS idx_amusement_group_bookings_session
    ON amusement_group_bookings (session_id, booking_status, created_at DESC);

CREATE INDEX IF NOT EXISTS idx_amusement_group_bookings_project
    ON amusement_group_bookings (project_id, booking_status, created_at DESC);

INSERT INTO permissions (permission_code, permission_name, domain, risk_level, requires_approval, status)
VALUES
    ('amusement.group_booking.view', 'View amusement group and private bookings', 'amusement', 'medium', FALSE, 'active'),
    ('amusement.group_booking.manage', 'Manage amusement group and private bookings', 'amusement', 'high', TRUE, 'active'),
    ('amusement.group_booking.approve', 'Approve amusement group and private booking capacity lock', 'amusement', 'high', TRUE, 'active')
ON CONFLICT (permission_code) DO UPDATE SET
    permission_name = EXCLUDED.permission_name,
    domain = EXCLUDED.domain,
    risk_level = EXCLUDED.risk_level,
    requires_approval = EXCLUDED.requires_approval,
    status = EXCLUDED.status;

INSERT INTO role_permissions (role_id, permission_code)
SELECT 'role_platform_admin', permission_code
  FROM permissions
 WHERE permission_code IN (
        'amusement.group_booking.view',
        'amusement.group_booking.manage',
        'amusement.group_booking.approve'
 )
ON CONFLICT (role_id, permission_code) DO NOTHING;
