-- R5 T2 + T3 动作前置闸口链（裁决⑥ 终端开关后端拒绝铁律 + scope 7 维；裁决⑧ 准入统一走 supervisor_review）。
-- 本迁移：
--   1) terminal_capability_switches 加 scope_type / terminal_type 取值 CHECK（幂等 DO 块；
--      取值集 = 现状代码/测试实际写入全集 ∪ 规格集，避免在既有数据上失败）。
--   2) 新建 supervisor_review_policies 策略表（外部化 SupervisorReviewService 原硬编码 List.of 之外的"准入闸"策略，三态 allow/warn/block）。
--   3) seed 四动作准入策略 + 三个权限点（terminal.switch.manage 已在 V010，此处补 admission.policy.manage / admission.review）。

-- ── 1) terminal_capability_switches CHECK ───────────────────────────────────
-- scope_type 取值集核对：现状代码 TerminalDeviceService 仅写 'global'/'device'，JDBC 集成测试写 'device'；
--   规格 §1.1 七维 = global/project/merchant/business_system/module/device/version（已含现状全集）。最终取规格 7 维。
-- terminal_type 取值集核对：现状代码默认 'kiosk'，设备注册 TERMINAL_TYPES = {staff_work_phone, tablet, kiosk}（注意是 'tablet' 不是 'staff_tablet'）；
--   规格 §1.1 集 = customer_app/customer_miniapp/web_admin/mobile_admin/staff_tablet/staff_work_phone/staff_personal_app/merchant_web/merchant_app/kiosk/*。
--   规格集缺现状的 'tablet'，故最终 = 现状全集 ∪ 规格集（额外补 'tablet'），否则既有 tablet 设备开关行会令 CHECK 失败。
DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM pg_constraint WHERE conname = 'chk_tcs_scope_type'
    ) THEN
        ALTER TABLE terminal_capability_switches
            ADD CONSTRAINT chk_tcs_scope_type CHECK (scope_type IN (
                'global', 'project', 'merchant', 'business_system', 'module', 'device', 'version'
            ));
    END IF;

    IF NOT EXISTS (
        SELECT 1 FROM pg_constraint WHERE conname = 'chk_tcs_terminal_type'
    ) THEN
        ALTER TABLE terminal_capability_switches
            ADD CONSTRAINT chk_tcs_terminal_type CHECK (terminal_type IN (
                'customer_app', 'customer_miniapp', 'web_admin', 'mobile_admin',
                'staff_tablet', 'staff_work_phone', 'staff_personal_app',
                'merchant_web', 'merchant_app', 'kiosk', 'tablet', '*'
            ));
    END IF;
END $$;

-- ── 2) supervisor_review_policies（准入闸策略表，规格 §2.1） ──────────────────
CREATE TABLE IF NOT EXISTS supervisor_review_policies (
    policy_code    VARCHAR(100) PRIMARY KEY,
    action_type    VARCHAR(100) NOT NULL,     -- 与 @GuardedAction.admissionAction 对应
    object_type    VARCHAR(100) NOT NULL,
    trigger_mode   VARCHAR(32)  NOT NULL,      -- always / over_contract_scope / over_space / expired / qualification_abnormal / permission_insufficient
    outcome        VARCHAR(16)  NOT NULL,      -- allow / warn / block 三态
    approval_level VARCHAR(32)  NOT NULL,      -- one_level_up / two_levels_up / top_authority / specific_role
    required_level INT          NOT NULL,
    enabled        BOOLEAN      NOT NULL DEFAULT true,
    created_at     TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at     TIMESTAMPTZ  NOT NULL DEFAULT now(),
    CONSTRAINT chk_srp_trigger CHECK (trigger_mode IN (
        'always', 'over_contract_scope', 'over_space', 'expired', 'qualification_abnormal', 'permission_insufficient'
    )),
    CONSTRAINT chk_srp_outcome CHECK (outcome IN ('allow', 'warn', 'block')),
    CONSTRAINT chk_srp_level CHECK (approval_level IN (
        'one_level_up', 'two_levels_up', 'top_authority', 'specific_role'
    ))
);

CREATE INDEX IF NOT EXISTS idx_supervisor_review_policies_action
    ON supervisor_review_policies (action_type, object_type, enabled);

-- seed 四动作策略（block/warn/allow 各示例；ON CONFLICT DO NOTHING）。
INSERT INTO supervisor_review_policies
    (policy_code, action_type, object_type, trigger_mode, outcome, approval_level, required_level, enabled)
VALUES
    ('bar_bill_waiver_block',        'bar.bill_waiver',         'bar_bill',     'always',                 'block', 'one_level_up',  2, true),
    ('amusement_reopen_block',       'amusement.reopen',        'inspection',   'qualification_abnormal', 'block', 'two_levels_up', 3, true),
    ('finance_locked_backfill_warn', 'finance.locked_backfill', 'ar_ap_item',   'always',                 'warn',  'one_level_up',  2, true),
    ('ticketing_special_release_allow','ticketing.special_release','verification','permission_insufficient','allow','one_level_up', 1, true)
ON CONFLICT (policy_code) DO NOTHING;

-- ── 3) 权限点 seed（terminal.switch.manage 已在 V010；补 admission.* 两个） ───
INSERT INTO permissions (permission_code, permission_name, domain, risk_level, requires_approval, status)
VALUES
    ('admission.policy.manage', 'Manage admission review policies', 'governance', 'high', TRUE,  'active'),
    ('admission.review',        'Approve admission blocked actions', 'governance', 'high', TRUE,  'active')
ON CONFLICT (permission_code) DO UPDATE SET
    permission_name = EXCLUDED.permission_name,
    domain = EXCLUDED.domain,
    risk_level = EXCLUDED.risk_level,
    requires_approval = EXCLUDED.requires_approval,
    status = EXCLUDED.status;

INSERT INTO role_permissions (role_id, permission_code)
SELECT 'role_platform_admin', permission_code
  FROM permissions
 WHERE permission_code IN ('admission.policy.manage', 'admission.review')
ON CONFLICT (role_id, permission_code) DO NOTHING;
