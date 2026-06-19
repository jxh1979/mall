-- R5 T9 MDM 自有侧后端（裁决⑨ 此前暂缓，现按用户要求落地；MDM 开关默认关）。
-- 口径（与 p0-action-event-evidence-matrix §3.1 / D10 收口一致）：
--   * MDM 开关 (managed_work_devices) **默认关**：开关关时，"未纳管设备禁用高风险动作"由
--     终端能力开关 + 人工准入代偿（已有口径），MDM 纳管/策略字段为预留、不阻断业务；
--     开关开时，未纳管设备的高风险动作经 MdmService 硬禁用。
--   * 本迁移落地：设备纳管字段 + 预装软件/网站白名单配置表 + 设备违规留痕表 + 开关默认关 + 权限。
-- 本迁移：
--   1) terminal_devices ADD management_status / policy_status / last_policy_sync_at / enrolled_at（CHECK 枚举）。
--   2) mdm_app_whitelist（预装/允许 app 包名）+ mdm_site_whitelist（允许访问域名），含 scope + enabled。
--   3) mdm_policy_violations（设备违规留痕：私装软件 / 访问非白名单 / 绕过企微 等）。
--   4) terminal_capability_switches 显式 seed managed_work_devices enabled=false（默认关；T2 无显式行=默认开，故须显式插 false）。
--   5) seed 权限 mdm.policy.manage / mdm.device.manage / mdm.violation.view。

-- ── 1) terminal_devices 纳管字段（幂等 ADD + CHECK 枚举） ─────────────────────
ALTER TABLE terminal_devices
    ADD COLUMN IF NOT EXISTS management_status VARCHAR(32) NOT NULL DEFAULT 'unmanaged',
    ADD COLUMN IF NOT EXISTS policy_status     VARCHAR(32),
    ADD COLUMN IF NOT EXISTS last_policy_sync_at TIMESTAMPTZ,
    ADD COLUMN IF NOT EXISTS enrolled_at       TIMESTAMPTZ;

DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM pg_constraint WHERE conname = 'chk_terminal_devices_management_status'
    ) THEN
        ALTER TABLE terminal_devices
            ADD CONSTRAINT chk_terminal_devices_management_status CHECK (
                management_status IN ('unmanaged', 'enrolled', 'compliant', 'violation')
            );
    END IF;

    IF NOT EXISTS (
        SELECT 1 FROM pg_constraint WHERE conname = 'chk_terminal_devices_policy_status'
    ) THEN
        ALTER TABLE terminal_devices
            ADD CONSTRAINT chk_terminal_devices_policy_status CHECK (
                policy_status IS NULL OR policy_status IN ('pending', 'applied', 'failed', 'outdated')
            );
    END IF;
END $$;

CREATE INDEX IF NOT EXISTS idx_terminal_devices_management_status
    ON terminal_devices (management_status);

-- ── 2) 预装软件清单 + 网站白名单配置表 ──────────────────────────────────────
-- scope_type 与 terminal_capability_switches 一致语义：global / project / device_model（设备型号）。
CREATE TABLE IF NOT EXISTS mdm_app_whitelist (
    id           VARCHAR(64) PRIMARY KEY,
    package_name VARCHAR(255) NOT NULL,        -- Android app 包名
    app_label    VARCHAR(255),
    scope_type   VARCHAR(32) NOT NULL DEFAULT 'global',
    scope_id     VARCHAR(128) NOT NULL DEFAULT 'global',
    enabled      BOOLEAN     NOT NULL DEFAULT TRUE,
    reason       TEXT,
    created_at   TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at   TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT uq_mdm_app_whitelist_scope UNIQUE (package_name, scope_type, scope_id),
    CONSTRAINT chk_mdm_app_whitelist_scope CHECK (scope_type IN ('global', 'project', 'device_model'))
);

CREATE INDEX IF NOT EXISTS idx_mdm_app_whitelist_enabled
    ON mdm_app_whitelist (enabled, scope_type, scope_id);

CREATE TABLE IF NOT EXISTS mdm_site_whitelist (
    id          VARCHAR(64) PRIMARY KEY,
    domain      VARCHAR(255) NOT NULL,         -- 允许访问域名
    description VARCHAR(255),
    scope_type  VARCHAR(32) NOT NULL DEFAULT 'global',
    scope_id    VARCHAR(128) NOT NULL DEFAULT 'global',
    enabled     BOOLEAN     NOT NULL DEFAULT TRUE,
    reason      TEXT,
    created_at  TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at  TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT uq_mdm_site_whitelist_scope UNIQUE (domain, scope_type, scope_id),
    CONSTRAINT chk_mdm_site_whitelist_scope CHECK (scope_type IN ('global', 'project', 'device_model'))
);

CREATE INDEX IF NOT EXISTS idx_mdm_site_whitelist_enabled
    ON mdm_site_whitelist (enabled, scope_type, scope_id);

-- ── 3) 设备违规留痕表 ───────────────────────────────────────────────────────
CREATE TABLE IF NOT EXISTS mdm_policy_violations (
    id             VARCHAR(64) PRIMARY KEY,
    device_id      VARCHAR(128) NOT NULL,       -- 对齐 terminal_devices.device_id（不强制 FK：违规可在未注册前发现）
    violation_type VARCHAR(64) NOT NULL,        -- unauthorized_app / non_whitelisted_site / wecom_bypass / root_detected / policy_sync_failed / other
    detail         TEXT,
    occurred_at    TIMESTAMPTZ NOT NULL DEFAULT now(),
    handled        BOOLEAN     NOT NULL DEFAULT FALSE,
    handled_note   TEXT,
    created_at     TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT chk_mdm_policy_violations_type CHECK (violation_type IN (
        'unauthorized_app', 'non_whitelisted_site', 'wecom_bypass',
        'root_detected', 'policy_sync_failed', 'other'
    ))
);

CREATE INDEX IF NOT EXISTS idx_mdm_policy_violations_device
    ON mdm_policy_violations (device_id, occurred_at DESC);
CREATE INDEX IF NOT EXISTS idx_mdm_policy_violations_handled
    ON mdm_policy_violations (handled, occurred_at DESC);

-- ── 4) MDM 能力开关默认关 ───────────────────────────────────────────────────
-- T2 TerminalSwitchGuard 语义：无显式行=默认开启（opt-out）。
--   故"MDM 默认关"必须显式插入 enabled=FALSE 行；terminal_type='*'、scope_type='global'。
--   capability_code='managed_work_devices'。取值集符合 V068 CHECK（terminal_type 含 '*'、scope_type 含 'global'）。
INSERT INTO terminal_capability_switches
    (id, terminal_type, capability_code, scope_type, scope_id, enabled, reason, created_at, updated_at)
VALUES
    ('tcs_mdm_managed_work_devices_off', '*', 'managed_work_devices', 'global', 'global', FALSE,
     'R5 T9 MDM 自有侧默认关闭：未纳管禁用由终端开关+人工准入代偿，AMAPI/商业 MDM 接入并验收后开启', now(), now())
ON CONFLICT (terminal_type, capability_code, scope_type, scope_id) DO NOTHING;

-- ── 5) 权限 seed ────────────────────────────────────────────────────────────
INSERT INTO permissions (permission_code, permission_name, domain, risk_level, requires_approval, status)
VALUES
    ('mdm.policy.manage',  'Manage MDM app/site whitelists',        'terminal', 'high', TRUE, 'active'),
    ('mdm.device.manage',  'Enroll/sync MDM managed devices',       'terminal', 'high', TRUE, 'active'),
    ('mdm.violation.view', 'View MDM device policy violations',     'terminal', 'high', TRUE, 'active')
ON CONFLICT (permission_code) DO UPDATE SET
    permission_name = EXCLUDED.permission_name,
    domain = EXCLUDED.domain,
    risk_level = EXCLUDED.risk_level,
    requires_approval = EXCLUDED.requires_approval,
    status = EXCLUDED.status;

INSERT INTO role_permissions (role_id, permission_code)
SELECT 'role_platform_admin', permission_code
  FROM permissions
 WHERE permission_code IN ('mdm.policy.manage', 'mdm.device.manage', 'mdm.violation.view')
ON CONFLICT (role_id, permission_code) DO NOTHING;
