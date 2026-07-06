-- V255 数据存档与审批后删除范式(试点:通票权益模板)。
-- 用户拍板(2026-07-06):"所有数据都应有存档或删除的能力,可要求审批,不能录入后就不可变。"
-- 首个试点 entitlement_templates:配错/过期模板可【存档】(软下线可恢复)或【删除】(硬删走上级审批)。
-- 设计详见 docs/data-archive-delete-pattern-design-v0.1.md。
--
-- 存档轴:archived_at 非空=已存档;列表默认 WHERE archived_at IS NULL 过滤(照 V182/V179 软删除先例)。
-- 删除轴:delete_status none->pending(申请)->approved(执行前瞬时态,随即物理DELETE);申请须先存档、审批禁自审、无活引用方可删。
-- 全列可空/带默认,向后兼容,可独立回滚。

ALTER TABLE entitlement_templates
    ADD COLUMN IF NOT EXISTS archived_at         TIMESTAMPTZ,
    ADD COLUMN IF NOT EXISTS archived_by         VARCHAR(64),
    ADD COLUMN IF NOT EXISTS delete_status       VARCHAR(16) NOT NULL DEFAULT 'none',
    ADD COLUMN IF NOT EXISTS delete_requested_by VARCHAR(64),
    ADD COLUMN IF NOT EXISTS delete_requested_at TIMESTAMPTZ,
    ADD COLUMN IF NOT EXISTS delete_reason       TEXT;

DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'chk_et_delete_status') THEN
        ALTER TABLE entitlement_templates
            ADD CONSTRAINT chk_et_delete_status
            CHECK (delete_status IN ('none','pending','approved'));
    END IF;
END $$;

-- 列表默认过滤走此偏索引(未存档 + 未审批通过删除)。
CREATE INDEX IF NOT EXISTS idx_entitlement_templates_active_list
    ON entitlement_templates (created_at DESC, id)
 WHERE archived_at IS NULL AND delete_status <> 'approved';

-- 权限码:审批权益模板删除(与既有 ticket.entitlement.manage 职责分离;发起权≠审批权,照 supervisor_review 原则)。
INSERT INTO permissions (permission_code, permission_name, domain, risk_level, requires_approval, status)
VALUES
    ('ticket.entitlement.approve', 'Approve ticket entitlement template deletion', 'ticketing', 'critical', TRUE, 'active')
ON CONFLICT (permission_code) DO UPDATE SET
    permission_name = EXCLUDED.permission_name,
    domain = EXCLUDED.domain,
    risk_level = EXCLUDED.risk_level,
    requires_approval = EXCLUDED.requires_approval,
    status = EXCLUDED.status;

INSERT INTO role_permissions (role_id, permission_code)
SELECT 'role_platform_admin', 'ticket.entitlement.approve'
ON CONFLICT (role_id, permission_code) DO NOTHING;
