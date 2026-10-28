-- 文件版本：V0.01
-- V568：第十八批 E 段第 22 条 商户证照到期预警与宽限期。
-- 依据 PGM:531「到期预警一律提前发出（业务参数，按证照类型可配，出厂默认提前三个月，须同时配置上下限）……预警同时送达商户侧，
--   商户在商户专区可查看本商户全部证照的到期情况。证照或资质到期后，可申请一个后台可配时长的宽限期（业务参数，按证照类型可配，
--   须同时配置上下限），宽限期须经最高权限审批后生效；宽限期内可继续经营但每日提醒，超过宽限期仍未更新的一律阻断，不得再次延长。」
-- 铁律：不为存量数据回填；HTML 没给宽限期出厂值，不预置（未配置的证照类型不能申请宽限）。

-- 1) 按证照类型配置：宽限天数与预警提前月数各自「值、下限、上限」三项同空或同填，同填时 0 ≤ 下限 ≤ 值 ≤ 上限（范式同 V560、V566）。
--    预警未配置（三项皆空）按出厂提前 3 个月；宽限未配置即该类型不能申请宽限。保险不适用宽限只在配置接口一处拦。
CREATE TABLE IF NOT EXISTS compliance_item_type_configs (
    item_type VARCHAR(40) PRIMARY KEY,
    grace_days INTEGER,
    grace_min_days INTEGER,
    grace_max_days INTEGER,
    warning_months INTEGER,
    warning_min_months INTEGER,
    warning_max_months INTEGER,
    updated_by VARCHAR(128) NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT chk_citc_item_type CHECK (item_type IN ('business_license', 'food_license', 'alcohol_license', 'fire_certificate',
        'special_equipment', 'insurance', 'supplier_qualification', 'inspection_certificate', 'custom')),
    CONSTRAINT chk_citc_grace CHECK (
        (grace_days IS NULL AND grace_min_days IS NULL AND grace_max_days IS NULL)
        OR (grace_days IS NOT NULL AND grace_min_days IS NOT NULL AND grace_max_days IS NOT NULL
            AND 0 <= grace_min_days AND grace_min_days <= grace_days AND grace_days <= grace_max_days)),
    CONSTRAINT chk_citc_warning CHECK (
        (warning_months IS NULL AND warning_min_months IS NULL AND warning_max_months IS NULL)
        OR (warning_months IS NOT NULL AND warning_min_months IS NOT NULL AND warning_max_months IS NOT NULL
            AND 0 <= warning_min_months AND warning_min_months <= warning_months AND warning_months <= warning_max_months))
);
COMMENT ON TABLE compliance_item_type_configs IS '证照到期宽限期与预警提前量（按证照类型、全平台统一；值与上下限同配）';
COMMENT ON COLUMN compliance_item_type_configs.grace_days IS '到期后可申请的宽限天数（未配置＝该类型不能申请宽限）';
COMMENT ON COLUMN compliance_item_type_configs.warning_months IS '到期预警提前月数（未配置按出厂 3 个月）';

-- 2) 证照条目上的宽限申请状态：申请时固化截止日 grace_until＝valid_to＋宽限天数；审批单号用于核对审批回写。
ALTER TABLE compliance_expiry_items ADD COLUMN IF NOT EXISTS grace_status VARCHAR(16);
ALTER TABLE compliance_expiry_items ADD COLUMN IF NOT EXISTS grace_until DATE;
ALTER TABLE compliance_expiry_items ADD COLUMN IF NOT EXISTS grace_review_id VARCHAR(64);
ALTER TABLE compliance_expiry_items DROP CONSTRAINT IF EXISTS chk_compliance_expiry_grace_status;
ALTER TABLE compliance_expiry_items ADD CONSTRAINT chk_compliance_expiry_grace_status
    CHECK (grace_status IS NULL OR grace_status IN ('pending_review', 'approved', 'rejected'));
COMMENT ON COLUMN compliance_expiry_items.grace_status IS '宽限申请状态：pending_review 待最高权限审批 / approved 已批准 / rejected 已驳回；续期登记后清空';
COMMENT ON COLUMN compliance_expiry_items.grace_until IS '宽限截止日（含当日），申请时固化＝有效期止＋宽限天数';
COMMENT ON COLUMN compliance_expiry_items.grace_review_id IS '宽限申请的最高权限审批单号（supervisor_review_decisions.review_id）';

-- 3) 宽限期内每日提醒：提醒类型加 grace（每日一行，沿用 V051 去重键）。
ALTER TABLE compliance_reminders DROP CONSTRAINT IF EXISTS chk_compliance_reminder_type;
ALTER TABLE compliance_reminders ADD CONSTRAINT chk_compliance_reminder_type
    CHECK (reminder_type IN ('upcoming', 'expired', 'grace'));

-- 4) 商户申请宽限期的权限码（商户角色由后台配置，仓库无商户角色种子）。
INSERT INTO permissions (permission_code, permission_name, domain, risk_level, requires_approval, status)
VALUES ('compliance.grace_period.request', '商户申请证照宽限期', 'compliance', 'medium', FALSE, 'active')
ON CONFLICT (permission_code) DO NOTHING;
INSERT INTO role_permissions (role_id, permission_code)
VALUES ('role_platform_admin', 'compliance.grace_period.request')
ON CONFLICT (role_id, permission_code) DO NOTHING;

DO $$
BEGIN
    IF EXISTS (SELECT 1 FROM pg_roles WHERE rolname = 'complex_app') THEN
        EXECUTE 'GRANT SELECT, INSERT, UPDATE, DELETE ON compliance_item_type_configs TO complex_app';
    END IF;
END $$;
