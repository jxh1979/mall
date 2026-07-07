-- V260 数据存档与审批后删除范式铺到【票务配置主数据 ticket_types + ticket_price_rules】(数据删除 Tier-A #2)。
-- 照 V255(权益模板试点)/V259(商户主数据)范式:配错/退场的票种、价格规则可【存档】(软下线可恢复)
-- 或【删除】(硬删走上级审批)。票种被订单/权益绑定/派生价规则等以 ticket_type_id 引用,
-- 价格规则被订单以 price_rule_id 引用,故硬删前的活引用校验尤为严格(见 countTicketTypeReferences/countPriceRuleReferences)。
--
-- 存档轴:archived_at 非空=已存档;列表默认 WHERE archived_at IS NULL 过滤。
-- 删除轴:delete_status none->pending(申请)->approved(执行前瞬时态,随即物理DELETE);申请须先存档、审批禁自审、无活引用方可删。
-- 全列可空/带默认,向后兼容,可独立回滚。

ALTER TABLE ticket_types
    ADD COLUMN IF NOT EXISTS archived_at         TIMESTAMPTZ,
    ADD COLUMN IF NOT EXISTS archived_by         VARCHAR(64),
    ADD COLUMN IF NOT EXISTS delete_status       VARCHAR(16) NOT NULL DEFAULT 'none',
    ADD COLUMN IF NOT EXISTS delete_requested_by VARCHAR(64),
    ADD COLUMN IF NOT EXISTS delete_requested_at TIMESTAMPTZ,
    ADD COLUMN IF NOT EXISTS delete_reason       TEXT;

ALTER TABLE ticket_price_rules
    ADD COLUMN IF NOT EXISTS archived_at         TIMESTAMPTZ,
    ADD COLUMN IF NOT EXISTS archived_by         VARCHAR(64),
    ADD COLUMN IF NOT EXISTS delete_status       VARCHAR(16) NOT NULL DEFAULT 'none',
    ADD COLUMN IF NOT EXISTS delete_requested_by VARCHAR(64),
    ADD COLUMN IF NOT EXISTS delete_requested_at TIMESTAMPTZ,
    ADD COLUMN IF NOT EXISTS delete_reason       TEXT;

DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'chk_ticket_type_delete_status') THEN
        ALTER TABLE ticket_types
            ADD CONSTRAINT chk_ticket_type_delete_status
            CHECK (delete_status IN ('none','pending','approved'));
    END IF;
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'chk_ticket_price_rule_delete_status') THEN
        ALTER TABLE ticket_price_rules
            ADD CONSTRAINT chk_ticket_price_rule_delete_status
            CHECK (delete_status IN ('none','pending','approved'));
    END IF;
END $$;

-- 列表默认过滤走此偏索引(未存档 + 未审批通过删除)。
CREATE INDEX IF NOT EXISTS idx_ticket_types_active_list
    ON ticket_types (created_at DESC, id)
 WHERE archived_at IS NULL AND delete_status <> 'approved';

CREATE INDEX IF NOT EXISTS idx_ticket_price_rules_active_list
    ON ticket_price_rules (created_at DESC, id)
 WHERE archived_at IS NULL AND delete_status <> 'approved';

-- 权限码:审批票种/价格规则删除。domain=ticketing(与既有 ticket.entitlement.approve 一致,见 V255);
-- 发起权(ticket.product.manage / ticket.price_variant.manage)≠审批权,照 supervisor_review 职责分离原则;critical + requires_approval。
INSERT INTO permissions (permission_code, permission_name, domain, risk_level, requires_approval, status)
VALUES
    ('ticket.type.approve', '审批票种删除', 'ticketing', 'critical', TRUE, 'active'),
    ('ticket.price_rule.approve', '审批票种价格规则删除', 'ticketing', 'critical', TRUE, 'active')
ON CONFLICT (permission_code) DO UPDATE SET
    permission_name = EXCLUDED.permission_name,
    domain = EXCLUDED.domain,
    risk_level = EXCLUDED.risk_level,
    requires_approval = EXCLUDED.requires_approval,
    status = EXCLUDED.status;

INSERT INTO role_permissions (role_id, permission_code)
SELECT 'role_platform_admin', 'ticket.type.approve'
ON CONFLICT (role_id, permission_code) DO NOTHING;

INSERT INTO role_permissions (role_id, permission_code)
SELECT 'role_platform_admin', 'ticket.price_rule.approve'
ON CONFLICT (role_id, permission_code) DO NOTHING;
