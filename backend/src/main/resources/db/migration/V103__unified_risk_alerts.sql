-- P0 清单#5 统一异常责任链（evidence-risk-layer-spec §8）。
-- 背景：risk_alert_actions 全仓零命中，exception.first_handled/reported/escalated/resolved/dismissed
-- 在 Java 侧从未发布，跨域异常仅散落在 3 张孤岛表（analytics_risk_alerts / inventory_risk_alerts /
-- amusement_incidents）。本迁移建立统一跨域汇聚层 risk_alerts + risk_alert_actions，提供一致的
-- 上报 → 认领 → 转交 → 升级 → 关闭（resolve/dismiss）责任链。孤岛表可后续桥接，本迁移不强制改动。

-- ── 统一风险预警主表（跨域汇聚层）──────────────────────────────────────────────
CREATE TABLE IF NOT EXISTS risk_alerts (
    alert_id VARCHAR(64) PRIMARY KEY,
    source_domain VARCHAR(32) NOT NULL,
    source_ref VARCHAR(128),
    alert_type VARCHAR(64) NOT NULL,
    risk_level VARCHAR(16) NOT NULL,
    status VARCHAR(16) NOT NULL,
    summary VARCHAR(500) NOT NULL,
    first_reported_by VARCHAR(64) NOT NULL,
    first_handler_id VARCHAR(64),
    escalated_to VARCHAR(64),
    resolved_by VARCHAR(64),
    resolution_conclusion TEXT,
    project_id VARCHAR(64),
    merchant_id VARCHAR(64),
    idempotency_key VARCHAR(128),
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,
    CONSTRAINT chk_risk_alerts_source_domain CHECK (source_domain IN (
        'ticketing', 'bar', 'amusement', 'inventory', 'finance',
        'wecom', 'device', 'thirdparty', 'analytics', 'platform'
    )),
    CONSTRAINT chk_risk_alerts_risk_level CHECK (risk_level IN ('normal', 'suspicious', 'high', 'critical')),
    CONSTRAINT chk_risk_alerts_status CHECK (status IN ('open', 'handling', 'escalated', 'resolved', 'dismissed')),
    -- "异常关闭必须有结论"：status=resolved 时 resolution_conclusion 必须非空。
    CONSTRAINT chk_risk_alerts_resolved_conclusion CHECK (
        status <> 'resolved'
        OR (resolution_conclusion IS NOT NULL AND length(btrim(resolution_conclusion)) > 0)
    )
);

CREATE INDEX IF NOT EXISTS idx_risk_alerts_domain_status
    ON risk_alerts (source_domain, status, risk_level, created_at DESC);

CREATE INDEX IF NOT EXISTS idx_risk_alerts_project
    ON risk_alerts (project_id, status, created_at DESC);

CREATE INDEX IF NOT EXISTS idx_risk_alerts_merchant
    ON risk_alerts (merchant_id, status, created_at DESC)
 WHERE merchant_id IS NOT NULL;

-- 上报幂等键（actor 维度在服务层拼接 operationId，键本身全局唯一即可）。
CREATE UNIQUE INDEX IF NOT EXISTS uq_risk_alerts_idempotency_key
    ON risk_alerts (idempotency_key)
 WHERE idempotency_key IS NOT NULL;

-- ── 责任链动作流水（append-only，永久留痕）────────────────────────────────────
CREATE TABLE IF NOT EXISTS risk_alert_actions (
    action_id VARCHAR(64) PRIMARY KEY,
    alert_id VARCHAR(64) NOT NULL REFERENCES risk_alerts(alert_id) ON DELETE CASCADE,
    action_type VARCHAR(16) NOT NULL,
    actor_id VARCHAR(64) NOT NULL,
    from_status VARCHAR(16),
    to_status VARCHAR(16) NOT NULL,
    reason TEXT,
    evidence_refs JSONB NOT NULL DEFAULT '[]'::jsonb,
    created_at TIMESTAMPTZ NOT NULL,
    CONSTRAINT chk_risk_alert_actions_type CHECK (action_type IN (
        'report', 'claim', 'transfer', 'escalate', 'resolve', 'dismiss'
    )),
    CONSTRAINT chk_risk_alert_actions_evidence CHECK (jsonb_typeof(evidence_refs) = 'array')
);

CREATE INDEX IF NOT EXISTS idx_risk_alert_actions_alert
    ON risk_alert_actions (alert_id, created_at ASC, action_id);

-- ── 权限点（责任链六动作）─────────────────────────────────────────────────────
INSERT INTO permissions (permission_code, permission_name, domain, risk_level, requires_approval, status)
VALUES
    ('risk_alert.report',   'Report a unified risk alert',          'evidence', 'medium', FALSE, 'active'),
    ('risk_alert.claim',    'Claim a unified risk alert',           'evidence', 'medium', FALSE, 'active'),
    ('risk_alert.transfer', 'Transfer a unified risk alert',        'evidence', 'high',   FALSE, 'active'),
    ('risk_alert.escalate', 'Escalate a unified risk alert',        'evidence', 'high',   FALSE, 'active'),
    ('risk_alert.resolve',  'Resolve a unified risk alert',         'evidence', 'high',   TRUE,  'active'),
    ('risk_alert.dismiss',  'Dismiss a unified risk alert',         'evidence', 'high',   TRUE,  'active'),
    ('risk_alert.view',     'View unified risk alerts',             'evidence', 'medium', FALSE, 'active')
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
        'risk_alert.report',
        'risk_alert.claim',
        'risk_alert.transfer',
        'risk_alert.escalate',
        'risk_alert.resolve',
        'risk_alert.dismiss',
        'risk_alert.view'
 )
ON CONFLICT (role_id, permission_code) DO NOTHING;
