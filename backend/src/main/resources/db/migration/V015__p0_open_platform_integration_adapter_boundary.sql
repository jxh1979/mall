CREATE TABLE IF NOT EXISTS open_app_credentials (
    id VARCHAR(64) PRIMARY KEY,
    app_key VARCHAR(128) NOT NULL,
    secret_ref VARCHAR(512) NOT NULL,
    status VARCHAR(32) NOT NULL,
    allowed_endpoint_keys JSONB NOT NULL DEFAULT '[]'::jsonb,
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,
    CONSTRAINT chk_open_app_credentials_status CHECK (status IN ('active', 'disabled')),
    CONSTRAINT chk_open_app_credentials_endpoint_keys CHECK (jsonb_typeof(allowed_endpoint_keys) = 'array')
);

CREATE UNIQUE INDEX IF NOT EXISTS uq_open_app_credentials_app_key
    ON open_app_credentials (app_key);

CREATE TABLE IF NOT EXISTS open_app_tokens (
    id VARCHAR(64) PRIMARY KEY,
    app_key VARCHAR(128) NOT NULL,
    token_hash VARCHAR(128) NOT NULL,
    issued_at TIMESTAMPTZ NOT NULL,
    expires_at TIMESTAMPTZ NOT NULL,
    status VARCHAR(32) NOT NULL,
    nonce VARCHAR(255) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL,
    CONSTRAINT chk_open_app_tokens_status CHECK (status IN ('active', 'revoked', 'expired')),
    CONSTRAINT chk_open_app_tokens_hash_length CHECK (length(token_hash) >= 64)
);

CREATE INDEX IF NOT EXISTS idx_open_app_tokens_app_status
    ON open_app_tokens (app_key, status, expires_at DESC);

CREATE TABLE IF NOT EXISTS open_app_nonce_replays (
    id VARCHAR(64) PRIMARY KEY,
    app_key VARCHAR(128) NOT NULL,
    nonce VARCHAR(255) NOT NULL,
    timestamp_value BIGINT NOT NULL,
    created_at TIMESTAMPTZ NOT NULL,
    CONSTRAINT uq_open_app_nonce_replays_app_nonce UNIQUE (app_key, nonce)
);

CREATE INDEX IF NOT EXISTS idx_open_app_nonce_replays_created
    ON open_app_nonce_replays (created_at DESC);

CREATE TABLE IF NOT EXISTS open_endpoint_configs (
    id VARCHAR(64) PRIMARY KEY,
    endpoint_key VARCHAR(128) NOT NULL,
    provider_code VARCHAR(128) NOT NULL,
    secret_ref VARCHAR(512) NOT NULL,
    status VARCHAR(32) NOT NULL,
    webhook_enabled BOOLEAN NOT NULL DEFAULT FALSE,
    polling_check_enabled BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,
    CONSTRAINT chk_open_endpoint_configs_status CHECK (status IN ('active', 'disabled'))
);

CREATE UNIQUE INDEX IF NOT EXISTS uq_open_endpoint_configs_endpoint_key
    ON open_endpoint_configs (endpoint_key);

CREATE INDEX IF NOT EXISTS idx_open_endpoint_configs_provider
    ON open_endpoint_configs (provider_code, status);

CREATE TABLE IF NOT EXISTS open_api_events (
    id VARCHAR(64) PRIMARY KEY,
    endpoint_key VARCHAR(128) NOT NULL,
    provider_code VARCHAR(128) NOT NULL,
    event_name VARCHAR(128) NOT NULL,
    idempotency_key VARCHAR(128) NOT NULL,
    signature_valid BOOLEAN NOT NULL,
    event_status VARCHAR(64) NOT NULL,
    payload_json JSONB NOT NULL DEFAULT '{}'::jsonb,
    result_json JSONB NOT NULL DEFAULT '{}'::jsonb,
    failure_reason TEXT,
    received_at TIMESTAMPTZ NOT NULL,
    processed_at TIMESTAMPTZ,
    CONSTRAINT uq_open_api_events_idempotency UNIQUE (endpoint_key, provider_code, idempotency_key),
    CONSTRAINT chk_open_api_events_payload CHECK (jsonb_typeof(payload_json) = 'object'),
    CONSTRAINT chk_open_api_events_result CHECK (jsonb_typeof(result_json) = 'object')
);

CREATE INDEX IF NOT EXISTS idx_open_api_events_endpoint_status
    ON open_api_events (endpoint_key, event_status, received_at DESC);

CREATE TABLE IF NOT EXISTS integration_provider_adapters (
    id VARCHAR(64) PRIMARY KEY,
    provider_code VARCHAR(128) NOT NULL,
    adapter_type VARCHAR(64) NOT NULL,
    mode VARCHAR(64) NOT NULL,
    capabilities JSONB NOT NULL DEFAULT '[]'::jsonb,
    environment VARCHAR(64) NOT NULL,
    status VARCHAR(64) NOT NULL,
    webhook_enabled BOOLEAN NOT NULL DEFAULT FALSE,
    polling_check_enabled BOOLEAN NOT NULL DEFAULT FALSE,
    field_mapping_json JSONB NOT NULL DEFAULT '{}'::jsonb,
    webhook_config_json JSONB NOT NULL DEFAULT '{}'::jsonb,
    polling_config_json JSONB NOT NULL DEFAULT '{}'::jsonb,
    credential_secret_ref VARCHAR(512),
    production_ready BOOLEAN NOT NULL DEFAULT FALSE,
    operator_note TEXT,
    evidence_attachment_ids JSONB NOT NULL DEFAULT '[]'::jsonb,
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,
    CONSTRAINT chk_integration_provider_adapters_type CHECK (adapter_type IN ('payment', 'ticket_channel', 'face_age', 'wecom', 'robot', 'video', 'device', 'retail', 'catering')),
    CONSTRAINT chk_integration_provider_adapters_mode CHECK (mode IN ('mock', 'sandbox', 'production')),
    CONSTRAINT chk_integration_provider_adapters_environment CHECK (environment IN ('mock', 'sandbox', 'production')),
    CONSTRAINT chk_integration_provider_adapters_status CHECK (status IN ('draft', 'mock_ready', 'test_ready', 'production_ready', 'disabled')),
    CONSTRAINT chk_integration_provider_adapters_capabilities CHECK (jsonb_typeof(capabilities) = 'array'),
    CONSTRAINT chk_integration_provider_adapters_mapping CHECK (jsonb_typeof(field_mapping_json) = 'object'),
    CONSTRAINT chk_integration_provider_adapters_webhook CHECK (jsonb_typeof(webhook_config_json) = 'object'),
    CONSTRAINT chk_integration_provider_adapters_polling CHECK (jsonb_typeof(polling_config_json) = 'object'),
    CONSTRAINT chk_integration_provider_adapters_evidence CHECK (jsonb_typeof(evidence_attachment_ids) = 'array'),
    CONSTRAINT chk_integration_provider_adapters_ready CHECK (
        production_ready = FALSE
        OR (
            status = 'production_ready'
            AND environment = 'production'
            AND webhook_enabled = TRUE
            AND polling_check_enabled = TRUE
            AND credential_secret_ref IS NOT NULL
            AND jsonb_array_length(capabilities) > 0
        )
    )
);

CREATE UNIQUE INDEX IF NOT EXISTS uq_integration_provider_adapters_provider_code
    ON integration_provider_adapters (provider_code);

CREATE INDEX IF NOT EXISTS idx_integration_provider_adapters_status
    ON integration_provider_adapters (status, environment, updated_at DESC);

CREATE TABLE IF NOT EXISTS integration_provider_adapter_tests (
    id VARCHAR(64) PRIMARY KEY,
    adapter_id VARCHAR(64) NOT NULL REFERENCES integration_provider_adapters(id) ON DELETE CASCADE,
    provider_code VARCHAR(128) NOT NULL,
    scenario VARCHAR(64) NOT NULL,
    mock_payload_json JSONB NOT NULL DEFAULT '{}'::jsonb,
    test_status VARCHAR(64) NOT NULL,
    result_json JSONB NOT NULL DEFAULT '{}'::jsonb,
    created_at TIMESTAMPTZ NOT NULL,
    CONSTRAINT chk_integration_provider_adapter_tests_scenario CHECK (scenario IN ('auth', 'webhook', 'polling', 'reconcile', 'field_mapping', 'failure_retry')),
    CONSTRAINT chk_integration_provider_adapter_tests_status CHECK (test_status IN ('passed', 'failed')),
    CONSTRAINT chk_integration_provider_adapter_tests_payload CHECK (jsonb_typeof(mock_payload_json) = 'object'),
    CONSTRAINT chk_integration_provider_adapter_tests_result CHECK (jsonb_typeof(result_json) = 'object')
);

CREATE INDEX IF NOT EXISTS idx_integration_provider_adapter_tests_adapter
    ON integration_provider_adapter_tests (adapter_id, created_at DESC);

CREATE TABLE IF NOT EXISTS integration_read_ledger (
    id VARCHAR(64) PRIMARY KEY,
    ledger_type VARCHAR(64) NOT NULL,
    provider_code VARCHAR(128) NOT NULL,
    business_id VARCHAR(128) NOT NULL,
    status VARCHAR(64) NOT NULL,
    payload_json JSONB NOT NULL DEFAULT '{}'::jsonb,
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,
    CONSTRAINT chk_integration_read_ledger_payload CHECK (jsonb_typeof(payload_json) = 'object')
);

CREATE INDEX IF NOT EXISTS idx_integration_read_ledger_type
    ON integration_read_ledger (ledger_type, provider_code, created_at DESC);

INSERT INTO permissions (permission_code, permission_name, domain, risk_level, requires_approval, status)
VALUES
    ('integration.provider_adapter.view', 'View integration provider adapters and ledgers', 'integration', 'medium', FALSE, 'active'),
    ('integration.provider_adapter.create', 'Create integration provider adapters', 'integration', 'high', TRUE, 'active'),
    ('integration.provider_adapter.update', 'Update integration provider adapters', 'integration', 'high', TRUE, 'active'),
    ('integration.provider_adapter.production_ready', 'Mark integration provider adapters production ready', 'integration', 'high', TRUE, 'active'),
    ('integration.provider_adapter.test', 'Run integration provider adapter mock tests', 'integration', 'medium', FALSE, 'active')
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
        'integration.provider_adapter.view',
        'integration.provider_adapter.create',
        'integration.provider_adapter.update',
        'integration.provider_adapter.production_ready',
        'integration.provider_adapter.test'
 )
ON CONFLICT (role_id, permission_code) DO NOTHING;

INSERT INTO open_app_credentials (
    id, app_key, secret_ref, status, allowed_endpoint_keys, created_at, updated_at
)
VALUES (
    'open_app_cred_p0',
    'p0-open-app',
    'manual-ledger:p0-open-app',
    'active',
    '["p0-default"]'::jsonb,
    now(),
    now()
)
ON CONFLICT (app_key) DO UPDATE SET
    secret_ref = EXCLUDED.secret_ref,
    status = EXCLUDED.status,
    allowed_endpoint_keys = EXCLUDED.allowed_endpoint_keys,
    updated_at = EXCLUDED.updated_at;

INSERT INTO open_endpoint_configs (
    id, endpoint_key, provider_code, secret_ref, status, webhook_enabled,
    polling_check_enabled, created_at, updated_at
)
VALUES (
    'open_endpoint_p0_default',
    'p0-default',
    'mock-provider',
    'manual-ledger:p0-webhook',
    'active',
    TRUE,
    TRUE,
    now(),
    now()
)
ON CONFLICT (endpoint_key) DO UPDATE SET
    provider_code = EXCLUDED.provider_code,
    secret_ref = EXCLUDED.secret_ref,
    status = EXCLUDED.status,
    webhook_enabled = EXCLUDED.webhook_enabled,
    polling_check_enabled = EXCLUDED.polling_check_enabled,
    updated_at = EXCLUDED.updated_at;

INSERT INTO integration_provider_adapters (
    id, provider_code, adapter_type, mode, capabilities, environment, status,
    webhook_enabled, polling_check_enabled, field_mapping_json, webhook_config_json,
    polling_config_json, credential_secret_ref, production_ready, operator_note,
    evidence_attachment_ids, created_at, updated_at
)
VALUES (
    'adapter_p0_mock_provider',
    'mock-provider',
    'payment',
    'mock',
    '["webhook", "polling", "field_mapping"]'::jsonb,
    'mock',
    'mock_ready',
    TRUE,
    TRUE,
    '{"provider_status": "status"}'::jsonb,
    '{"enabled": true}'::jsonb,
    '{"enabled": true}'::jsonb,
    'manual-ledger:p0-provider',
    FALSE,
    'P0 mock adapter boundary seed',
    '[]'::jsonb,
    now(),
    now()
)
ON CONFLICT (provider_code) DO UPDATE SET
    adapter_type = EXCLUDED.adapter_type,
    mode = EXCLUDED.mode,
    capabilities = EXCLUDED.capabilities,
    environment = EXCLUDED.environment,
    status = EXCLUDED.status,
    webhook_enabled = EXCLUDED.webhook_enabled,
    polling_check_enabled = EXCLUDED.polling_check_enabled,
    field_mapping_json = EXCLUDED.field_mapping_json,
    webhook_config_json = EXCLUDED.webhook_config_json,
    polling_config_json = EXCLUDED.polling_config_json,
    credential_secret_ref = EXCLUDED.credential_secret_ref,
    production_ready = EXCLUDED.production_ready,
    operator_note = EXCLUDED.operator_note,
    updated_at = EXCLUDED.updated_at;

INSERT INTO integration_read_ledger (
    id, ledger_type, provider_code, business_id, status, payload_json, created_at, updated_at
)
VALUES
    ('ledger_p0_compensation_task', 'compensation_task', 'mock-provider', 'comp_task_1', 'open', '{"reason": "manual compensation placeholder"}'::jsonb, now(), now()),
    ('ledger_p0_device', 'device', 'mock-provider', 'device_1', 'online', '{"device_id": "device_1"}'::jsonb, now(), now()),
    ('ledger_p0_device_event', 'device_event', 'mock-provider', 'device_event_1', 'received', '{"event": "heartbeat"}'::jsonb, now(), now()),
    ('ledger_p0_offline_sync_batch', 'offline_sync_batch', 'mock-provider', 'offline_batch_1', 'pending', '{"batch_no": "offline_batch_1"}'::jsonb, now(), now()),
    ('ledger_p0_order_sync', 'order_sync', 'mock-provider', 'order_sync_1', 'synced', '{"order_id": "order_sync_1"}'::jsonb, now(), now()),
    ('ledger_p0_retail_sku', 'retail_sku', 'mock-provider', 'sku_1', 'active', '{"sku": "sku_1"}'::jsonb, now(), now()),
    ('ledger_p0_retry_task', 'retry_task', 'mock-provider', 'retry_1', 'waiting', '{"retry_count": 0}'::jsonb, now(), now()),
    ('ledger_p0_robot_task_event', 'robot_task_event', 'mock-provider', 'robot_event_1', 'received', '{"event": "task_update"}'::jsonb, now(), now()),
    ('ledger_p0_robot_task', 'robot_task', 'mock-provider', 'robot_task_1', 'queued', '{"task": "delivery"}'::jsonb, now(), now()),
    ('ledger_p0_video_camera', 'video_camera', 'mock-provider', 'camera_1', 'online', '{"camera_id": "camera_1"}'::jsonb, now(), now()),
    ('ledger_p0_voucher_verification', 'voucher_verification', 'mock-provider', 'voucher_1', 'verified', '{"voucher_id": "voucher_1"}'::jsonb, now(), now()),
    ('ledger_p0_wecom_binding', 'wecom_binding', 'mock-provider', 'wecom_binding_1', 'bound', '{"external_user_id": "external_1"}'::jsonb, now(), now()),
    ('ledger_p0_wecom_conversation', 'wecom_conversation', 'mock-provider', 'wecom_conv_1', 'open', '{"conversation_id": "wecom_conv_1"}'::jsonb, now(), now()),
    ('ledger_p0_wecom_message', 'wecom_message', 'mock-provider', 'wecom_msg_1', 'received', '{"message_id": "wecom_msg_1"}'::jsonb, now(), now()),
    ('ledger_p0_wecom_risk_hit', 'wecom_risk_hit', 'mock-provider', 'wecom_risk_1', 'review_required', '{"risk_code": "keyword"}'::jsonb, now(), now())
ON CONFLICT (id) DO UPDATE SET
    ledger_type = EXCLUDED.ledger_type,
    provider_code = EXCLUDED.provider_code,
    business_id = EXCLUDED.business_id,
    status = EXCLUDED.status,
    payload_json = EXCLUDED.payload_json,
    updated_at = EXCLUDED.updated_at;
