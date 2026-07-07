-- V259 数据存档与审批后删除范式铺到【商户主数据 merchants】。
-- 照 V255(权益模板试点)范式:配错/退场商户可【存档】(软下线可恢复)或【删除】(硬删走上级审批)。
-- 商户=主数据,被全系统约 40 张表以 merchant_id 引用,故硬删前的活引用校验尤为严格(见 countMerchantReferences)。
--
-- 存档轴:archived_at 非空=已存档;列表默认 WHERE archived_at IS NULL 过滤。
-- 删除轴:delete_status none->pending(申请)->approved(执行前瞬时态,随即物理DELETE);申请须先存档、审批禁自审、无活引用方可删。
-- 全列可空/带默认,向后兼容,可独立回滚。

ALTER TABLE merchants
    ADD COLUMN IF NOT EXISTS archived_at         TIMESTAMPTZ,
    ADD COLUMN IF NOT EXISTS archived_by         VARCHAR(64),
    ADD COLUMN IF NOT EXISTS delete_status       VARCHAR(16) NOT NULL DEFAULT 'none',
    ADD COLUMN IF NOT EXISTS delete_requested_by VARCHAR(64),
    ADD COLUMN IF NOT EXISTS delete_requested_at TIMESTAMPTZ,
    ADD COLUMN IF NOT EXISTS delete_reason       TEXT;

DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'chk_merchant_delete_status') THEN
        ALTER TABLE merchants
            ADD CONSTRAINT chk_merchant_delete_status
            CHECK (delete_status IN ('none','pending','approved'));
    END IF;
END $$;

-- 列表默认过滤走此偏索引(未存档 + 未审批通过删除)。
CREATE INDEX IF NOT EXISTS idx_merchants_active_list
    ON merchants (created_at DESC, id)
 WHERE archived_at IS NULL AND delete_status <> 'approved';

-- 权限码:审批商户删除。domain=platform(与既有 merchant.profile.view/manage 一致,见 V005);
-- 发起权(merchant.profile.manage)≠审批权,照 supervisor_review 职责分离原则;critical + requires_approval。
INSERT INTO permissions (permission_code, permission_name, domain, risk_level, requires_approval, status)
VALUES
    ('merchant.profile.approve', '审批商户删除', 'platform', 'critical', TRUE, 'active')
ON CONFLICT (permission_code) DO UPDATE SET
    permission_name = EXCLUDED.permission_name,
    domain = EXCLUDED.domain,
    risk_level = EXCLUDED.risk_level,
    requires_approval = EXCLUDED.requires_approval,
    status = EXCLUDED.status;

INSERT INTO role_permissions (role_id, permission_code)
SELECT 'role_platform_admin', 'merchant.profile.approve'
ON CONFLICT (role_id, permission_code) DO NOTHING;
