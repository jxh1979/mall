CREATE TABLE IF NOT EXISTS bar_production_ticket_events (
    id VARCHAR(80) PRIMARY KEY,
    ticket_id VARCHAR(64) NOT NULL REFERENCES bar_production_tickets(id) ON DELETE CASCADE,
    order_id VARCHAR(64) NOT NULL REFERENCES bar_orders(id) ON DELETE CASCADE,
    bar_id VARCHAR(64) NOT NULL,
    action_type VARCHAR(32) NOT NULL,
    from_status VARCHAR(64) NOT NULL,
    to_status VARCHAR(64) NOT NULL,
    station_code VARCHAR(80),
    operator_user_id VARCHAR(64) NOT NULL,
    operator_note VARCHAR(500) NOT NULL,
    idempotency_key VARCHAR(128) NOT NULL,
    evidence_attachment_ids JSONB NOT NULL DEFAULT '[]'::jsonb,
    created_at TIMESTAMPTZ NOT NULL,
    CONSTRAINT chk_bar_production_ticket_event_action CHECK (
        action_type IN ('accept', 'start_prepare', 'mark_ready', 'cancel', 'exception', 'deliver')
    ),
    CONSTRAINT chk_bar_production_ticket_event_status CHECK (
        to_status IN ('accepted', 'preparing', 'ready', 'cancelled', 'exception', 'delivered')
    )
);

CREATE UNIQUE INDEX IF NOT EXISTS uq_bar_production_ticket_events_idempotency
    ON bar_production_ticket_events (ticket_id, idempotency_key);

CREATE INDEX IF NOT EXISTS idx_bar_production_ticket_events_ticket_created
    ON bar_production_ticket_events (ticket_id, created_at DESC);

CREATE INDEX IF NOT EXISTS idx_bar_production_ticket_events_order
    ON bar_production_ticket_events (order_id, created_at DESC);

INSERT INTO permissions (permission_code, permission_name, domain, risk_level, requires_approval, status)
VALUES
    ('bar.production_ticket.view', 'View bar production tickets and events', 'bar', 'medium', FALSE, 'active'),
    ('bar.production_ticket.manage', 'Transition bar production ticket workflow status', 'bar', 'high', TRUE, 'active')
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
        'bar.production_ticket.view',
        'bar.production_ticket.manage'
 )
ON CONFLICT (role_id, permission_code) DO NOTHING;
