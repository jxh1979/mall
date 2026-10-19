-- 文件版本：V0.01
-- HP-C03-0 押金收取月数业务参数（全平台统一）。依据：BSM L1347「商户押金的收取月数全平台统一（业务参数，全平台统一可配，出厂默认三个月，
-- 须同时配置上下限；下限不得低于三个月，配置校验拒绝更低的值，不允许零押金或负押金）。押金金额 ＝ 收取月数 × 该商户签约时的月租金」；
-- 2026-10-18 操作员裁决第 3 条「本批补齐 BSM L1347」。范式同 V413 staff_policy_settings：'global' 出厂行，未配置时采用出厂值；
-- 上下限留空，首次配置必须同时填写（不自拟上下限出厂数值）。
CREATE TABLE contract_deposit_policy (
    scope_id VARCHAR(16) PRIMARY KEY CHECK (scope_id = 'global'),
    deposit_months INTEGER NOT NULL,
    min_months INTEGER,
    max_months INTEGER,
    updated_by VARCHAR(128),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT chk_contract_deposit_policy_floor CHECK (deposit_months >= 3),
    CONSTRAINT chk_contract_deposit_policy_bounds CHECK (
        (min_months IS NULL AND max_months IS NULL)
        OR (min_months IS NOT NULL AND max_months IS NOT NULL
            AND min_months >= 3 AND min_months <= deposit_months AND deposit_months <= max_months))
);
INSERT INTO contract_deposit_policy (scope_id, deposit_months) VALUES ('global', 3);

-- 仅平台可改（服务层另校验平台范围）：配置权限挂平台管理员。
INSERT INTO permissions (permission_code, permission_name, domain, risk_level, requires_approval, status)
VALUES ('contract.deposit_months.manage', '配置押金收取月数（全平台）', 'platform', 'high', TRUE, 'active')
ON CONFLICT (permission_code) DO NOTHING;
INSERT INTO role_permissions (role_id, permission_code)
VALUES ('role_platform_admin', 'contract.deposit_months.manage')
ON CONFLICT (role_id, permission_code) DO NOTHING;
