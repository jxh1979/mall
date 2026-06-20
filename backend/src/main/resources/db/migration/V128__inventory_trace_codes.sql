-- V128：溯源码主数据（inventory_trace_codes）建表 + 码 CRUD 权限点（REQ-39 收口）。
--
-- 背景（审计实证）：R4-G1 DRIFT —— 矩阵明确声称 inventory_trace_codes 表，125 个迁移从未建；
-- trace_code 此前仅作共用字符串列散落在 stock_movements / inventory_trace_handovers。本迁移建码主数据表，
-- 承载“酒入库贴码（登记）→ 出库打印码 → 二维码串责任链查询”闭环（REQ-39）。与相邻能力互补、不混淆：
--   - V059 三层溯源 evidence/TraceabilityService；V074 五段交接链 inventory/TraceHandoverRecord；
--   - 已有端点 POST /trace-codes/scan-handover（扫码交接，非码主数据）。本表是“码主数据 CRUD”。
-- 关键：带直接 merchant_id NOT NULL 列（供 domain-ownership 自动归类 direct scope，与本域既有强制 merchant 注入一致）。

-- ── 1) 溯源码主数据表 ────────────────────────────────────────────────────────
-- status 生命周期：registered（贴码登记/入库）→ printed（出库打印）→ bound（绑定单据）→ consumed（核销）。
-- bound_ref：出库绑定的单据引用（可空）；printed_at：打印时间戳（可空，打印时回填）。
CREATE TABLE IF NOT EXISTS inventory_trace_codes (
    id VARCHAR(64) PRIMARY KEY,
    trace_code VARCHAR(128) NOT NULL UNIQUE,            -- 码值（唯一，贴码/打印/查询主键）
    item_id VARCHAR(64) NOT NULL,                       -- 物料/商品 id（供应链域只读引用）
    batch_id VARCHAR(64),                               -- 批次（可空）
    merchant_id VARCHAR(64) NOT NULL,                   -- 直接归属列（domain-ownership direct scope）
    status VARCHAR(32) NOT NULL DEFAULT 'registered',   -- registered|printed|bound|consumed
    bound_ref VARCHAR(128),                             -- 出库绑定的单据引用（可空）
    printed_at TIMESTAMPTZ,                             -- 出库打印时间（可空）
    created_by VARCHAR(64),
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT chk_inventory_trace_code_status
        CHECK (status IN ('registered', 'printed', 'bound', 'consumed'))
);

-- trace_code UNIQUE 已由列约束建索引；补 merchant_id / item_id 查询索引。
CREATE INDEX IF NOT EXISTS idx_inventory_trace_codes_merchant
    ON inventory_trace_codes (merchant_id);

CREATE INDEX IF NOT EXISTS idx_inventory_trace_codes_item
    ON inventory_trace_codes (item_id);

-- ── 2) 码 CRUD 权限点（domain=inventory）。 ──
-- 照 V126/V127 形态：register/print 为操作类（normal），view 为只读。授予 role_platform_admin。
INSERT INTO permissions (permission_code, permission_name, domain, risk_level, requires_approval, status)
VALUES
    ('inventory.trace_code.register',
     'Register an inventory trace code (inbound labeling)',
     'inventory', 'normal', FALSE, 'active'),
    ('inventory.trace_code.print',
     'Print an inventory trace code (outbound)',
     'inventory', 'normal', FALSE, 'active'),
    ('inventory.trace_code.view',
     'View inventory trace codes and responsibility chain',
     'inventory', 'normal', FALSE, 'active')
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
        'inventory.trace_code.register',
        'inventory.trace_code.print',
        'inventory.trace_code.view'
 )
ON CONFLICT (role_id, permission_code) DO NOTHING;
