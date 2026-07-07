-- V263 酒吧门店主数据（bars）：把此前到处自由填的 bar_id 字符串收敛为一张独立门店实体表。
-- 用户拍板(2026-07-06)："酒吧=独立门店实体，挂项目(综合体)，可选绑经营商户和所在空间"；每个项目可能十来个酒吧。
--
-- 目标：① 建 bars 主数据（可新建/管理），供各处手填的"酒吧 ID"改成从已建酒吧下拉选；
--       ② 回填历史：现有 bar_* / service_orders 里出现过的 bar_id 全部落成一条 bars 行，避免成孤儿、选择器能显示。
--
-- 存档/删除范式照 V255 权益模板 / V259 商户 / V260 票种 / V262 酒吧目录：
--   archived_at 非空=已存档（软下线可恢复，列表默认过滤）；
--   delete_status none->pending（申请，须先存档）->approved（执行前瞬时态，随即物理 DELETE，禁自审、无活引用方可删）。
-- 全列可空/带默认，向后兼容，可独立回滚。

CREATE TABLE IF NOT EXISTS bars (
    id                  VARCHAR(64) PRIMARY KEY,
    -- project_id 可空：新建时前端必填；可空是为兼容回填的历史 bar_id（当时无项目归属）。
    project_id          VARCHAR(64),
    merchant_id         VARCHAR(64),
    space_id            VARCHAR(64),
    bar_name            VARCHAR(120) NOT NULL,
    bar_code            VARCHAR(64),
    -- 默认低消（分），可空。
    minimum_spend_cent  BIGINT,
    status              VARCHAR(32) NOT NULL DEFAULT 'active',
    -- 存档/删除范式列。
    archived_at         TIMESTAMPTZ,
    archived_by         VARCHAR(64),
    delete_status       VARCHAR(32) NOT NULL DEFAULT 'none',
    delete_requested_by VARCHAR(64),
    delete_requested_at TIMESTAMPTZ,
    delete_reason       TEXT,
    created_at          TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at          TIMESTAMPTZ NOT NULL DEFAULT now()
);

DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'chk_bars_delete_status') THEN
        ALTER TABLE bars
            ADD CONSTRAINT chk_bars_delete_status
            CHECK (delete_status IN ('none','pending','approved'));
    END IF;
END $$;

-- 未存档未 approved 列表偏索引（照 V262 先例）。
CREATE INDEX IF NOT EXISTS idx_bars_active_list
    ON bars (created_at DESC, id)
 WHERE archived_at IS NULL AND delete_status <> 'approved';
-- 按项目/商户 scope 过滤用。
CREATE INDEX IF NOT EXISTS idx_bars_project ON bars (project_id);
CREATE INDEX IF NOT EXISTS idx_bars_merchant ON bars (merchant_id);

-- ============ 数据回填（关键，别丢历史）============
-- 先种 bar-main（前端历史默认写死值），再把各运营表里所有 DISTINCT bar_id 但不在 bars 里的，
-- 回填成一条 bars（name 用 bar_id 占位、project_id 空），使现有数据不成孤儿、选择器能显示。
INSERT INTO bars (id, bar_name, status, project_id, created_at, updated_at)
VALUES ('bar-main', '主酒吧', 'active', NULL, now(), now())
ON CONFLICT (id) DO NOTHING;

-- 各表带 bar_id 列的历史值 union 去重，name 用 bar_id 占位。
INSERT INTO bars (id, bar_name, status, project_id, created_at, updated_at)
SELECT DISTINCT bar_id, bar_id, 'active', NULL, now(), now()
  FROM (
        SELECT bar_id FROM bar_tables
        UNION SELECT bar_id FROM bar_products
        UNION SELECT bar_id FROM bar_product_categories
        UNION SELECT bar_id FROM bar_combos
        UNION SELECT bar_id FROM bar_reservations
        UNION SELECT bar_id FROM bar_orders
        UNION SELECT bar_id FROM bar_bills
        UNION SELECT bar_id FROM bar_table_sessions
        UNION SELECT bar_id FROM bar_shifts
        UNION SELECT bar_id FROM bar_deposit_rules
        UNION SELECT bar_id FROM bar_gift_redemptions
        UNION SELECT bar_id FROM bar_order_item_voids
        UNION SELECT bar_id FROM bar_production_tickets
        UNION SELECT bar_id FROM bar_production_ticket_events
        UNION SELECT bar_id FROM bar_reservation_invitation_members
        UNION SELECT bar_id FROM bar_table_guest_checkins
        UNION SELECT bar_id FROM bar_table_session_adjustments
        UNION SELECT bar_id FROM service_orders
  ) t
 WHERE bar_id IS NOT NULL
   AND bar_id <> ''
   AND bar_id NOT IN (SELECT id FROM bars);

-- ============ 权限码（3 个，domain=bar；approve=critical+requires_approval）============
INSERT INTO permissions (permission_code, permission_name, domain, risk_level, requires_approval, status)
VALUES
    ('bar.venue.view', '查看酒吧门店', 'bar', 'medium', FALSE, 'active'),
    ('bar.venue.manage', '管理酒吧门店', 'bar', 'high', FALSE, 'active'),
    ('bar.venue.approve', '审批酒吧门店删除', 'bar', 'critical', TRUE, 'active')
ON CONFLICT (permission_code) DO UPDATE SET
    permission_name = EXCLUDED.permission_name,
    domain = EXCLUDED.domain,
    risk_level = EXCLUDED.risk_level,
    requires_approval = EXCLUDED.requires_approval,
    status = EXCLUDED.status;

-- 新权限晚于 V261 全量授权基线，须自行补授 role_platform_admin（照 V262 注意事项）。
INSERT INTO role_permissions (role_id, permission_code)
SELECT 'role_platform_admin', permission_code
  FROM permissions
 WHERE permission_code IN ('bar.venue.view', 'bar.venue.manage', 'bar.venue.approve')
ON CONFLICT (role_id, permission_code) DO NOTHING;
