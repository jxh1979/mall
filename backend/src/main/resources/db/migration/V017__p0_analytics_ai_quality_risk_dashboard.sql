CREATE TABLE IF NOT EXISTS analytics_metric_definitions (
    metric_key VARCHAR(128) PRIMARY KEY,
    name VARCHAR(120) NOT NULL,
    domain VARCHAR(64) NOT NULL,
    unit VARCHAR(32) NOT NULL,
    calculation_note TEXT NOT NULL,
    owner_role VARCHAR(64) NOT NULL,
    status VARCHAR(32) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,
    CONSTRAINT chk_analytics_metric_definitions_status CHECK (status IN ('active', 'disabled'))
);

CREATE INDEX IF NOT EXISTS idx_analytics_metric_definitions_domain
    ON analytics_metric_definitions (domain, status);

CREATE TABLE IF NOT EXISTS analytics_dashboard_snapshots (
    snapshot_key VARCHAR(128) PRIMARY KEY,
    project_id VARCHAR(64) NOT NULL,
    business_date DATE NOT NULL,
    revenue_amount_cents BIGINT NOT NULL,
    order_count INTEGER NOT NULL,
    risk_alert_count INTEGER NOT NULL,
    critical_risk_count INTEGER NOT NULL,
    payload_json JSONB NOT NULL DEFAULT '{}'::jsonb,
    created_at TIMESTAMPTZ NOT NULL,
    CONSTRAINT chk_analytics_dashboard_snapshot_money CHECK (revenue_amount_cents >= 0),
    CONSTRAINT chk_analytics_dashboard_snapshot_counts CHECK (
        order_count >= 0
        AND risk_alert_count >= 0
        AND critical_risk_count >= 0
    )
);

CREATE INDEX IF NOT EXISTS idx_analytics_dashboard_snapshots_date
    ON analytics_dashboard_snapshots (project_id, business_date DESC, created_at DESC);

CREATE TABLE IF NOT EXISTS analytics_merchant_summaries (
    merchant_id VARCHAR(64) PRIMARY KEY,
    merchant_name VARCHAR(120) NOT NULL,
    merchant_type VARCHAR(64) NOT NULL,
    revenue_amount_cents BIGINT NOT NULL,
    order_count INTEGER NOT NULL,
    risk_alert_count INTEGER NOT NULL,
    settlement_status VARCHAR(64) NOT NULL,
    business_date DATE NOT NULL,
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,
    CONSTRAINT chk_analytics_merchant_summary_money CHECK (revenue_amount_cents >= 0),
    CONSTRAINT chk_analytics_merchant_summary_counts CHECK (order_count >= 0 AND risk_alert_count >= 0)
);

CREATE INDEX IF NOT EXISTS idx_analytics_merchant_summaries_date
    ON analytics_merchant_summaries (business_date DESC, revenue_amount_cents DESC);

CREATE TABLE IF NOT EXISTS analytics_merchant_health_scores (
    merchant_id VARCHAR(64) PRIMARY KEY,
    score INTEGER NOT NULL,
    score_level VARCHAR(64) NOT NULL,
    risk_count INTEGER NOT NULL,
    overdue_ar_amount_cents BIGINT NOT NULL,
    last_reviewed_at TIMESTAMPTZ NOT NULL,
    summary TEXT NOT NULL,
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,
    CONSTRAINT chk_analytics_merchant_health_score CHECK (score BETWEEN 0 AND 100),
    CONSTRAINT chk_analytics_merchant_health_counts CHECK (risk_count >= 0 AND overdue_ar_amount_cents >= 0)
);

CREATE INDEX IF NOT EXISTS idx_analytics_merchant_health_scores_level
    ON analytics_merchant_health_scores (score_level, score DESC);

CREATE TABLE IF NOT EXISTS analytics_metric_anomalies (
    id VARCHAR(64) PRIMARY KEY,
    metric_key VARCHAR(128) NOT NULL REFERENCES analytics_metric_definitions(metric_key),
    domain VARCHAR(64) NOT NULL,
    risk_level VARCHAR(32) NOT NULL,
    observed_value NUMERIC(20, 4) NOT NULL,
    expected_value NUMERIC(20, 4) NOT NULL,
    summary TEXT NOT NULL,
    status VARCHAR(32) NOT NULL,
    detected_at TIMESTAMPTZ NOT NULL,
    resolved_at TIMESTAMPTZ,
    CONSTRAINT chk_analytics_metric_anomalies_risk CHECK (risk_level IN ('normal', 'suspicious', 'high', 'critical')),
    CONSTRAINT chk_analytics_metric_anomalies_status CHECK (status IN ('open', 'resolved', 'ignored'))
);

CREATE INDEX IF NOT EXISTS idx_analytics_metric_anomalies_status
    ON analytics_metric_anomalies (status, risk_level, detected_at DESC);

CREATE TABLE IF NOT EXISTS analytics_risk_rules (
    rule_id VARCHAR(64) PRIMARY KEY,
    rule_code VARCHAR(128) NOT NULL UNIQUE,
    rule_name VARCHAR(120) NOT NULL,
    domain VARCHAR(64) NOT NULL,
    risk_level VARCHAR(32) NOT NULL,
    enabled BOOLEAN NOT NULL DEFAULT TRUE,
    rule_config_json JSONB NOT NULL DEFAULT '{}'::jsonb,
    action_note TEXT NOT NULL,
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,
    CONSTRAINT chk_analytics_risk_rules_risk CHECK (risk_level IN ('normal', 'suspicious', 'high', 'critical'))
);

CREATE INDEX IF NOT EXISTS idx_analytics_risk_rules_enabled
    ON analytics_risk_rules (enabled, domain, risk_level);

CREATE TABLE IF NOT EXISTS analytics_risk_alerts (
    id VARCHAR(64) PRIMARY KEY,
    rule_id VARCHAR(64) REFERENCES analytics_risk_rules(rule_id),
    alert_type VARCHAR(64) NOT NULL,
    risk_level VARCHAR(32) NOT NULL,
    object_type VARCHAR(64) NOT NULL,
    object_id VARCHAR(64),
    summary VARCHAR(500) NOT NULL,
    status VARCHAR(32) NOT NULL,
    operator_note TEXT,
    evidence_attachment_ids_json JSONB NOT NULL DEFAULT '[]'::jsonb,
    idempotency_key VARCHAR(128),
    created_at TIMESTAMPTZ NOT NULL,
    resolved_at TIMESTAMPTZ,
    updated_at TIMESTAMPTZ NOT NULL,
    CONSTRAINT chk_analytics_risk_alerts_risk CHECK (risk_level IN ('normal', 'suspicious', 'high', 'critical')),
    CONSTRAINT chk_analytics_risk_alerts_status CHECK (status IN ('open', 'investigating', 'resolved', 'dismissed')),
    CONSTRAINT chk_analytics_risk_alerts_evidence CHECK (jsonb_typeof(evidence_attachment_ids_json) = 'array')
);

CREATE INDEX IF NOT EXISTS idx_analytics_risk_alerts_status
    ON analytics_risk_alerts (status, risk_level, created_at DESC);

CREATE UNIQUE INDEX IF NOT EXISTS uq_analytics_risk_alerts_idempotency_key
    ON analytics_risk_alerts (idempotency_key)
    WHERE idempotency_key IS NOT NULL;

CREATE TABLE IF NOT EXISTS analytics_ai_quality_tasks (
    task_id VARCHAR(64) PRIMARY KEY,
    source_type VARCHAR(64) NOT NULL,
    source_id VARCHAR(64) NOT NULL,
    processing_mode VARCHAR(64) NOT NULL,
    model_provider VARCHAR(64) NOT NULL,
    model_version VARCHAR(128) NOT NULL,
    status VARCHAR(32) NOT NULL,
    requested_by VARCHAR(64),
    created_at TIMESTAMPTZ NOT NULL,
    completed_at TIMESTAMPTZ,
    CONSTRAINT chk_analytics_ai_quality_tasks_mode CHECK (processing_mode IN ('manual_review', 'rule_engine', 'external_ai_pending')),
    CONSTRAINT chk_analytics_ai_quality_tasks_status CHECK (status IN ('pending', 'running', 'completed', 'failed'))
);

CREATE INDEX IF NOT EXISTS idx_analytics_ai_quality_tasks_source
    ON analytics_ai_quality_tasks (source_type, source_id, created_at DESC);

CREATE TABLE IF NOT EXISTS analytics_ai_quality_results (
    result_id VARCHAR(64) PRIMARY KEY,
    task_id VARCHAR(64) NOT NULL REFERENCES analytics_ai_quality_tasks(task_id) ON DELETE CASCADE,
    source_type VARCHAR(64) NOT NULL,
    source_id VARCHAR(64) NOT NULL,
    quality_score INTEGER NOT NULL,
    risk_level VARCHAR(32) NOT NULL,
    confidence_score NUMERIC(5, 4) NOT NULL,
    summary TEXT NOT NULL,
    finding_count INTEGER NOT NULL,
    created_at TIMESTAMPTZ NOT NULL,
    CONSTRAINT chk_analytics_ai_quality_results_score CHECK (quality_score BETWEEN 0 AND 100),
    CONSTRAINT chk_analytics_ai_quality_results_confidence CHECK (confidence_score >= 0 AND confidence_score <= 1),
    CONSTRAINT chk_analytics_ai_quality_results_findings CHECK (finding_count >= 0),
    CONSTRAINT chk_analytics_ai_quality_results_risk CHECK (risk_level IN ('normal', 'suspicious', 'high', 'critical'))
);

CREATE INDEX IF NOT EXISTS idx_analytics_ai_quality_results_source
    ON analytics_ai_quality_results (source_type, source_id, created_at DESC);

CREATE TABLE IF NOT EXISTS analytics_customer_intents (
    intent_id VARCHAR(64) PRIMARY KEY,
    source_type VARCHAR(64) NOT NULL,
    source_id VARCHAR(64) NOT NULL,
    customer_ref VARCHAR(64) NOT NULL,
    intent_type VARCHAR(64) NOT NULL,
    confidence_score NUMERIC(5, 4) NOT NULL,
    risk_level VARCHAR(32) NOT NULL,
    summary TEXT NOT NULL,
    created_at TIMESTAMPTZ NOT NULL,
    CONSTRAINT chk_analytics_customer_intents_confidence CHECK (confidence_score >= 0 AND confidence_score <= 1),
    CONSTRAINT chk_analytics_customer_intents_risk CHECK (risk_level IN ('normal', 'suspicious', 'high', 'critical'))
);

CREATE INDEX IF NOT EXISTS idx_analytics_customer_intents_customer
    ON analytics_customer_intents (customer_ref, created_at DESC);

CREATE TABLE IF NOT EXISTS analytics_robot_metrics (
    metric_id VARCHAR(64) PRIMARY KEY,
    robot_id VARCHAR(64) NOT NULL,
    provider_code VARCHAR(64) NOT NULL,
    task_count INTEGER NOT NULL,
    success_count INTEGER NOT NULL,
    failure_count INTEGER NOT NULL,
    business_date DATE NOT NULL,
    summary TEXT NOT NULL,
    created_at TIMESTAMPTZ NOT NULL,
    CONSTRAINT chk_analytics_robot_metrics_counts CHECK (
        task_count >= 0
        AND success_count >= 0
        AND failure_count >= 0
        AND task_count >= success_count
        AND task_count >= failure_count
    )
);

CREATE INDEX IF NOT EXISTS idx_analytics_robot_metrics_date
    ON analytics_robot_metrics (business_date DESC, robot_id);

CREATE TABLE IF NOT EXISTS analytics_staff_service_scores (
    score_id VARCHAR(64) PRIMARY KEY,
    staff_id VARCHAR(64) NOT NULL,
    staff_name VARCHAR(120) NOT NULL,
    role_code VARCHAR(64) NOT NULL,
    service_score INTEGER NOT NULL,
    complaint_count INTEGER NOT NULL,
    positive_event_count INTEGER NOT NULL,
    risk_level VARCHAR(32) NOT NULL,
    business_date DATE NOT NULL,
    created_at TIMESTAMPTZ NOT NULL,
    CONSTRAINT chk_analytics_staff_service_scores_score CHECK (service_score BETWEEN 0 AND 100),
    CONSTRAINT chk_analytics_staff_service_scores_counts CHECK (complaint_count >= 0 AND positive_event_count >= 0),
    CONSTRAINT chk_analytics_staff_service_scores_risk CHECK (risk_level IN ('normal', 'suspicious', 'high', 'critical'))
);

CREATE INDEX IF NOT EXISTS idx_analytics_staff_service_scores_date
    ON analytics_staff_service_scores (business_date DESC, service_score DESC);

INSERT INTO permissions (permission_code, permission_name, domain, risk_level, requires_approval, status)
VALUES
    ('analytics.dashboard.view', 'View analytics dashboards', 'analytics', 'medium', FALSE, 'active'),
    ('analytics.risk_alert.view', 'View analytics risk alerts and AI quality ledgers', 'analytics', 'medium', FALSE, 'active'),
    ('analytics.risk_alert.manage', 'Manage analytics risk alerts', 'analytics', 'high', TRUE, 'active')
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
        'analytics.dashboard.view',
        'analytics.risk_alert.view',
        'analytics.risk_alert.manage'
 )
ON CONFLICT (role_id, permission_code) DO NOTHING;

INSERT INTO analytics_metric_definitions (
    metric_key, name, domain, unit, calculation_note, owner_role, status, created_at, updated_at
)
VALUES (
    'gmv_amount_cents',
    'GMV',
    'finance',
    'cent',
    'sum paid order amount in cents',
    'finance_manager',
    'active',
    '2026-06-14T00:00:00Z',
    '2026-06-14T00:00:00Z'
)
ON CONFLICT (metric_key) DO UPDATE SET
    name = EXCLUDED.name,
    domain = EXCLUDED.domain,
    unit = EXCLUDED.unit,
    calculation_note = EXCLUDED.calculation_note,
    owner_role = EXCLUDED.owner_role,
    status = EXCLUDED.status,
    updated_at = EXCLUDED.updated_at;

INSERT INTO analytics_dashboard_snapshots (
    snapshot_key, project_id, business_date, revenue_amount_cents, order_count,
    risk_alert_count, critical_risk_count, payload_json, created_at
)
VALUES (
    'p0_overview_today',
    'project_1',
    '2026-06-14',
    128800,
    23,
    2,
    1,
    '{"top_domain":"bar","source":"seed"}'::jsonb,
    '2026-06-14T00:00:00Z'
)
ON CONFLICT (snapshot_key) DO UPDATE SET
    project_id = EXCLUDED.project_id,
    business_date = EXCLUDED.business_date,
    revenue_amount_cents = EXCLUDED.revenue_amount_cents,
    order_count = EXCLUDED.order_count,
    risk_alert_count = EXCLUDED.risk_alert_count,
    critical_risk_count = EXCLUDED.critical_risk_count,
    payload_json = EXCLUDED.payload_json,
    created_at = EXCLUDED.created_at;

INSERT INTO analytics_merchant_summaries (
    merchant_id, merchant_name, merchant_type, revenue_amount_cents, order_count,
    risk_alert_count, settlement_status, business_date, created_at, updated_at
)
VALUES (
    'merchant_p0',
    'Blue Bar',
    'bar',
    128800,
    23,
    1,
    'settled',
    '2026-06-14',
    '2026-06-14T00:00:00Z',
    '2026-06-14T00:00:00Z'
)
ON CONFLICT (merchant_id) DO UPDATE SET
    merchant_name = EXCLUDED.merchant_name,
    merchant_type = EXCLUDED.merchant_type,
    revenue_amount_cents = EXCLUDED.revenue_amount_cents,
    order_count = EXCLUDED.order_count,
    risk_alert_count = EXCLUDED.risk_alert_count,
    settlement_status = EXCLUDED.settlement_status,
    business_date = EXCLUDED.business_date,
    updated_at = EXCLUDED.updated_at;

INSERT INTO analytics_merchant_health_scores (
    merchant_id, score, score_level, risk_count, overdue_ar_amount_cents,
    last_reviewed_at, summary, created_at, updated_at
)
VALUES (
    'merchant_p0',
    86,
    'healthy',
    1,
    0,
    '2026-06-14T00:00:00Z',
    'stable merchant operations',
    '2026-06-14T00:00:00Z',
    '2026-06-14T00:00:00Z'
)
ON CONFLICT (merchant_id) DO UPDATE SET
    score = EXCLUDED.score,
    score_level = EXCLUDED.score_level,
    risk_count = EXCLUDED.risk_count,
    overdue_ar_amount_cents = EXCLUDED.overdue_ar_amount_cents,
    last_reviewed_at = EXCLUDED.last_reviewed_at,
    summary = EXCLUDED.summary,
    updated_at = EXCLUDED.updated_at;

INSERT INTO analytics_metric_anomalies (
    id, metric_key, domain, risk_level, observed_value, expected_value,
    summary, status, detected_at, resolved_at
)
VALUES (
    'anomaly_p0',
    'gmv_amount_cents',
    'finance',
    'suspicious',
    128800,
    188800,
    'GMV below expected range',
    'open',
    '2026-06-14T00:00:00Z',
    NULL
)
ON CONFLICT (id) DO UPDATE SET
    metric_key = EXCLUDED.metric_key,
    domain = EXCLUDED.domain,
    risk_level = EXCLUDED.risk_level,
    observed_value = EXCLUDED.observed_value,
    expected_value = EXCLUDED.expected_value,
    summary = EXCLUDED.summary,
    status = EXCLUDED.status,
    detected_at = EXCLUDED.detected_at,
    resolved_at = EXCLUDED.resolved_at;

INSERT INTO analytics_risk_rules (
    rule_id, rule_code, rule_name, domain, risk_level, enabled,
    rule_config_json, action_note, created_at, updated_at
)
VALUES (
    'rule_p0_gmv_drop',
    'GMV_DROP',
    'GMV drop',
    'finance',
    'high',
    TRUE,
    '{"threshold_percent":30}'::jsonb,
    'notify manager',
    '2026-06-14T00:00:00Z',
    '2026-06-14T00:00:00Z'
)
ON CONFLICT (rule_id) DO UPDATE SET
    rule_code = EXCLUDED.rule_code,
    rule_name = EXCLUDED.rule_name,
    domain = EXCLUDED.domain,
    risk_level = EXCLUDED.risk_level,
    enabled = EXCLUDED.enabled,
    rule_config_json = EXCLUDED.rule_config_json,
    action_note = EXCLUDED.action_note,
    updated_at = EXCLUDED.updated_at;

INSERT INTO analytics_risk_alerts (
    id, rule_id, alert_type, risk_level, object_type, object_id, summary,
    status, operator_note, evidence_attachment_ids_json, idempotency_key,
    created_at, resolved_at, updated_at
)
VALUES (
    'alert_p0',
    'rule_p0_gmv_drop',
    'metric_anomaly',
    'high',
    'merchant',
    'merchant_p0',
    'Revenue dropped below threshold',
    'open',
    'seeded risk alert',
    '["att_seed"]'::jsonb,
    'seed-alert-p0',
    '2026-06-14T00:00:00Z',
    NULL,
    '2026-06-14T00:00:00Z'
)
ON CONFLICT (id) DO UPDATE SET
    rule_id = EXCLUDED.rule_id,
    alert_type = EXCLUDED.alert_type,
    risk_level = EXCLUDED.risk_level,
    object_type = EXCLUDED.object_type,
    object_id = EXCLUDED.object_id,
    summary = EXCLUDED.summary,
    status = EXCLUDED.status,
    operator_note = EXCLUDED.operator_note,
    evidence_attachment_ids_json = EXCLUDED.evidence_attachment_ids_json,
    idempotency_key = EXCLUDED.idempotency_key,
    resolved_at = EXCLUDED.resolved_at,
    updated_at = EXCLUDED.updated_at;

INSERT INTO analytics_ai_quality_tasks (
    task_id, source_type, source_id, processing_mode, model_provider,
    model_version, status, requested_by, created_at, completed_at
)
VALUES (
    'ai_task_p0',
    'ticket_call',
    'call_p0',
    'manual_review',
    'none',
    'none',
    'completed',
    'staff_1',
    '2026-06-14T00:00:00Z',
    '2026-06-14T00:05:00Z'
)
ON CONFLICT (task_id) DO UPDATE SET
    source_type = EXCLUDED.source_type,
    source_id = EXCLUDED.source_id,
    processing_mode = EXCLUDED.processing_mode,
    model_provider = EXCLUDED.model_provider,
    model_version = EXCLUDED.model_version,
    status = EXCLUDED.status,
    requested_by = EXCLUDED.requested_by,
    completed_at = EXCLUDED.completed_at;

INSERT INTO analytics_ai_quality_results (
    result_id, task_id, source_type, source_id, quality_score, risk_level,
    confidence_score, summary, finding_count, created_at
)
VALUES (
    'ai_result_p0',
    'ai_task_p0',
    'ticket_call',
    'call_p0',
    92,
    'normal',
    0.9100,
    'clear service interaction',
    1,
    '2026-06-14T00:05:00Z'
)
ON CONFLICT (result_id) DO UPDATE SET
    task_id = EXCLUDED.task_id,
    source_type = EXCLUDED.source_type,
    source_id = EXCLUDED.source_id,
    quality_score = EXCLUDED.quality_score,
    risk_level = EXCLUDED.risk_level,
    confidence_score = EXCLUDED.confidence_score,
    summary = EXCLUDED.summary,
    finding_count = EXCLUDED.finding_count,
    created_at = EXCLUDED.created_at;

INSERT INTO analytics_customer_intents (
    intent_id, source_type, source_id, customer_ref, intent_type,
    confidence_score, risk_level, summary, created_at
)
VALUES (
    'intent_p0',
    'ticket_call',
    'call_p0',
    'customer_p0',
    'refund_request',
    0.7600,
    'suspicious',
    'customer asks for refund',
    '2026-06-14T00:06:00Z'
)
ON CONFLICT (intent_id) DO UPDATE SET
    source_type = EXCLUDED.source_type,
    source_id = EXCLUDED.source_id,
    customer_ref = EXCLUDED.customer_ref,
    intent_type = EXCLUDED.intent_type,
    confidence_score = EXCLUDED.confidence_score,
    risk_level = EXCLUDED.risk_level,
    summary = EXCLUDED.summary,
    created_at = EXCLUDED.created_at;

INSERT INTO analytics_robot_metrics (
    metric_id, robot_id, provider_code, task_count, success_count,
    failure_count, business_date, summary, created_at
)
VALUES (
    'robot_metric_p0',
    'robot_1',
    'mock_provider',
    10,
    9,
    1,
    '2026-06-14',
    'one failed delivery task',
    '2026-06-14T00:00:00Z'
)
ON CONFLICT (metric_id) DO UPDATE SET
    robot_id = EXCLUDED.robot_id,
    provider_code = EXCLUDED.provider_code,
    task_count = EXCLUDED.task_count,
    success_count = EXCLUDED.success_count,
    failure_count = EXCLUDED.failure_count,
    business_date = EXCLUDED.business_date,
    summary = EXCLUDED.summary,
    created_at = EXCLUDED.created_at;

INSERT INTO analytics_staff_service_scores (
    score_id, staff_id, staff_name, role_code, service_score, complaint_count,
    positive_event_count, risk_level, business_date, created_at
)
VALUES (
    'staff_score_p0',
    'staff_1',
    'Alex',
    'frontdesk',
    95,
    0,
    12,
    'normal',
    '2026-06-14',
    '2026-06-14T00:00:00Z'
)
ON CONFLICT (score_id) DO UPDATE SET
    staff_id = EXCLUDED.staff_id,
    staff_name = EXCLUDED.staff_name,
    role_code = EXCLUDED.role_code,
    service_score = EXCLUDED.service_score,
    complaint_count = EXCLUDED.complaint_count,
    positive_event_count = EXCLUDED.positive_event_count,
    risk_level = EXCLUDED.risk_level,
    business_date = EXCLUDED.business_date,
    created_at = EXCLUDED.created_at;
