-- 文件版本：V0.02
-- M010-017/M010-029：复用一张表存放两项明确要求可配置的员工业务参数。
CREATE TABLE staff_policy_settings (
    policy_key VARCHAR(64) NOT NULL CHECK (policy_key IN ('shift_visible_days', 'payroll_penalty_cap_bps')),
    scope_id VARCHAR(128) NOT NULL,
    numeric_value INTEGER NOT NULL,
    min_value INTEGER NOT NULL,
    max_value INTEGER NOT NULL,
    updated_by VARCHAR(128),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    PRIMARY KEY (policy_key, scope_id),
    CHECK (min_value <= numeric_value AND numeric_value <= max_value),
    CHECK ((policy_key = 'shift_visible_days' AND min_value >= 1)
        OR (policy_key = 'payroll_penalty_cap_bps' AND scope_id = 'global' AND min_value >= 0 AND max_value <= 10000))
);
INSERT INTO staff_policy_settings(policy_key, scope_id, numeric_value, min_value, max_value)
VALUES ('payroll_penalty_cap_bps', 'global', 2000, 0, 10000);
-- U05-005/U10-003（BSM L1444「出厂默认 7 天，须同时配置上下限」；BSM-001 未填上下限的参数不得上线）：
-- 可见天数全局出厂行；上下限 1..31 为操作员 2026-09-10 裁定的上线配置；各经营主体的 merchant:/company: 行缺失时回落到本行。
INSERT INTO staff_policy_settings(policy_key, scope_id, numeric_value, min_value, max_value)
VALUES ('shift_visible_days', 'global', 7, 1, 31);
