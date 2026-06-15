CREATE TABLE IF NOT EXISTS ticket_dynamic_pricing_rules (
    id VARCHAR(64) PRIMARY KEY,
    ticket_type_id VARCHAR(64) NOT NULL REFERENCES ticket_types(id) ON DELETE CASCADE,
    rule_name VARCHAR(160) NOT NULL,
    priority INTEGER NOT NULL DEFAULT 0,
    effective_from DATE NOT NULL,
    effective_to DATE NOT NULL,
    sales_channel VARCHAR(64),
    adjustment_type VARCHAR(32) NOT NULL,
    adjustment_rate NUMERIC(8, 2),
    adjustment_amount_cent BIGINT,
    min_price_cent BIGINT,
    max_price_cent BIGINT,
    status VARCHAR(32) NOT NULL,
    condition_rule_json JSONB NOT NULL DEFAULT '{}'::jsonb,
    operator_note TEXT,
    evidence_attachment_ids JSONB NOT NULL DEFAULT '[]'::jsonb,
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,
    CONSTRAINT chk_ticket_dynamic_pricing_dates CHECK (effective_to >= effective_from),
    CONSTRAINT chk_ticket_dynamic_pricing_adjustment_type CHECK (adjustment_type IN ('percent_delta', 'fixed_delta')),
    CONSTRAINT chk_ticket_dynamic_pricing_status CHECK (status IN ('draft', 'published', 'disabled')),
    CONSTRAINT chk_ticket_dynamic_pricing_condition_json CHECK (jsonb_typeof(condition_rule_json) = 'object'),
    CONSTRAINT chk_ticket_dynamic_pricing_evidence CHECK (jsonb_typeof(evidence_attachment_ids) = 'array')
);

CREATE INDEX IF NOT EXISTS idx_ticket_dynamic_pricing_rules_match
    ON ticket_dynamic_pricing_rules (ticket_type_id, status, effective_from, effective_to, sales_channel, priority DESC);

INSERT INTO permissions (permission_code, permission_name, domain, risk_level, requires_approval, status)
VALUES
    ('ticket.dynamic_pricing.view', 'View ticket dynamic pricing rules', 'ticketing', 'medium', FALSE, 'active'),
    ('ticket.dynamic_pricing.manage', 'Manage ticket dynamic pricing rules', 'ticketing', 'high', TRUE, 'active'),
    ('ticket.dynamic_pricing.publish', 'Publish ticket dynamic pricing rules', 'ticketing', 'high', TRUE, 'active'),
    ('ticket.dynamic_pricing.simulate', 'Simulate ticket dynamic pricing rules', 'ticketing', 'medium', FALSE, 'active')
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
        'ticket.dynamic_pricing.view',
        'ticket.dynamic_pricing.manage',
        'ticket.dynamic_pricing.publish',
        'ticket.dynamic_pricing.simulate'
 )
ON CONFLICT (role_id, permission_code) DO NOTHING;
