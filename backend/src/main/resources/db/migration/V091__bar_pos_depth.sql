-- 第二阶段：成熟 POS 深度（对照 PRD §10.1）。四组能力落库：
--   (1) 退菜退酒 void/return item   bar_order_items
--   (2) 反结 re-open settled bill    bar_bills
--   (3) 沽清 86/out-of-stock         bar_products
--   (4) 小费 tip                      bar_bills
--
-- 设计要点：
--   - 出品前撤单（pending）不计库存损耗；已出品退酒（delivered）按 return_to_stock 回补库存或计损耗，
--     资金从账单扣除（结账前减应付，结账后走 refund 冲正——服务层据账单状态分流）。
--   - 反结允许 settled→reopened→重结的状态机；与 T7 日结锁定协同：服务层在反结前校验经营日是否锁定（DAY_CLOSE_LOCKED）。
--   - 沽清后下单服务层拒（PRODUCT_SOLD_OUT）；恢复供应解除。
--   - 小费进账单 payable 与结算，口径注明：小费为代收（归属服务员/交班结算），不计入低消与提成基数。
--   - 所有原因/责任人/时点留痕，退菜退酒事件入 bar_order_item_voids 流水。

-- ============================================================================
-- (1) 退菜退酒：bar_order_items 加撤单标记
-- ============================================================================
ALTER TABLE bar_order_items
    ADD COLUMN IF NOT EXISTS voided BOOLEAN NOT NULL DEFAULT FALSE,
    ADD COLUMN IF NOT EXISTS void_reason VARCHAR(500),
    ADD COLUMN IF NOT EXISTS voided_at TIMESTAMPTZ,
    ADD COLUMN IF NOT EXISTS voided_by_user_id VARCHAR(64),
    ADD COLUMN IF NOT EXISTS return_to_stock BOOLEAN NOT NULL DEFAULT FALSE;

-- 撤单一致性：voided=TRUE 时必须有原因与时点；未撤单不得携带退菜元数据（防误聚合）。
DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM information_schema.table_constraints
         WHERE constraint_name = 'chk_bar_order_items_void'
           AND table_name = 'bar_order_items'
    ) THEN
        ALTER TABLE bar_order_items
            ADD CONSTRAINT chk_bar_order_items_void CHECK (
                (voided = TRUE AND void_reason IS NOT NULL AND voided_at IS NOT NULL)
                OR (voided = FALSE AND void_reason IS NULL AND voided_at IS NULL
                    AND voided_by_user_id IS NULL AND return_to_stock = FALSE)
            );
    END IF;
END $$;

-- 退菜退酒流水（区分出品前撤单 cancel / 出品后退酒 return；记原因、责任人、是否回补库存、资金扣减、库存流水关联）。
CREATE TABLE IF NOT EXISTS bar_order_item_voids (
    id VARCHAR(64) PRIMARY KEY,
    order_item_id VARCHAR(64) NOT NULL REFERENCES bar_order_items(id) ON DELETE CASCADE,
    order_id VARCHAR(64) NOT NULL REFERENCES bar_orders(id) ON DELETE CASCADE,
    bar_id VARCHAR(64) NOT NULL,
    merchant_id VARCHAR(64),
    void_type VARCHAR(32) NOT NULL,
    quantity INTEGER NOT NULL,
    amount_cent BIGINT NOT NULL,
    was_delivered BOOLEAN NOT NULL,
    return_to_stock BOOLEAN NOT NULL DEFAULT FALSE,
    inventory_movement_id VARCHAR(64),
    bill_id VARCHAR(64) REFERENCES bar_bills(id),
    bill_adjustment_id VARCHAR(64),
    reason VARCHAR(500) NOT NULL,
    requested_by_user_id VARCHAR(64) NOT NULL,
    approval_required BOOLEAN NOT NULL DEFAULT TRUE,
    status VARCHAR(32) NOT NULL DEFAULT 'applied',
    idempotency_key VARCHAR(128) NOT NULL,
    evidence_attachment_ids JSONB NOT NULL DEFAULT '[]'::jsonb,
    created_at TIMESTAMPTZ NOT NULL,
    CONSTRAINT uq_bar_order_item_voids_idem UNIQUE (order_item_id, idempotency_key),
    CONSTRAINT chk_bar_order_item_voids_type CHECK (void_type IN ('cancel', 'return')),
    CONSTRAINT chk_bar_order_item_voids_quantity CHECK (quantity > 0),
    CONSTRAINT chk_bar_order_item_voids_amount CHECK (amount_cent >= 0)
);

CREATE INDEX IF NOT EXISTS idx_bar_order_item_voids_order
    ON bar_order_item_voids (order_id, created_at DESC);

-- ============================================================================
-- (2) 反结：bar_bills 加 reopen 状态机字段
-- ============================================================================
ALTER TABLE bar_bills
    ADD COLUMN IF NOT EXISTS reopened BOOLEAN NOT NULL DEFAULT FALSE,
    ADD COLUMN IF NOT EXISTS reopened_by_user_id VARCHAR(64),
    ADD COLUMN IF NOT EXISTS reopened_at TIMESTAMPTZ,
    ADD COLUMN IF NOT EXISTS reopen_reason VARCHAR(500),
    ADD COLUMN IF NOT EXISTS reopen_count INTEGER NOT NULL DEFAULT 0,
    -- (4) 小费一并落于账单（结算 payable 含小费；代收口径见文件头）。
    ADD COLUMN IF NOT EXISTS tip_amount_cent BIGINT NOT NULL DEFAULT 0;

DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM information_schema.table_constraints
         WHERE constraint_name = 'chk_bar_bills_reopen'
           AND table_name = 'bar_bills'
    ) THEN
        ALTER TABLE bar_bills
            ADD CONSTRAINT chk_bar_bills_reopen CHECK (
                (reopened = TRUE AND reopened_at IS NOT NULL AND reopen_reason IS NOT NULL)
                OR (reopened = FALSE)
            );
    END IF;
    IF NOT EXISTS (
        SELECT 1 FROM information_schema.table_constraints
         WHERE constraint_name = 'chk_bar_bills_reopen_count'
           AND table_name = 'bar_bills'
    ) THEN
        ALTER TABLE bar_bills
            ADD CONSTRAINT chk_bar_bills_reopen_count CHECK (reopen_count >= 0);
    END IF;
    IF NOT EXISTS (
        SELECT 1 FROM information_schema.table_constraints
         WHERE constraint_name = 'chk_bar_bills_tip'
           AND table_name = 'bar_bills'
    ) THEN
        ALTER TABLE bar_bills
            ADD CONSTRAINT chk_bar_bills_tip CHECK (tip_amount_cent >= 0);
    END IF;
END $$;

-- ============================================================================
-- (3) 沽清 / 估清：bar_products 加 86 标记
-- ============================================================================
ALTER TABLE bar_products
    ADD COLUMN IF NOT EXISTS sold_out BOOLEAN NOT NULL DEFAULT FALSE,
    ADD COLUMN IF NOT EXISTS sold_out_at TIMESTAMPTZ,
    ADD COLUMN IF NOT EXISTS sold_out_reason VARCHAR(500);

DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM information_schema.table_constraints
         WHERE constraint_name = 'chk_bar_products_sold_out'
           AND table_name = 'bar_products'
    ) THEN
        ALTER TABLE bar_products
            ADD CONSTRAINT chk_bar_products_sold_out CHECK (
                (sold_out = TRUE AND sold_out_at IS NOT NULL)
                OR (sold_out = FALSE AND sold_out_at IS NULL)
            );
    END IF;
END $$;

CREATE INDEX IF NOT EXISTS idx_bar_products_sold_out
    ON bar_products (bar_id, sold_out)
    WHERE sold_out = TRUE;

-- ============================================================================
-- 权限点 seed（退菜退酒/反结=高危需审批；沽清/小费 medium 不需审批）
-- ============================================================================
INSERT INTO permissions (permission_code, permission_name, domain, risk_level, requires_approval, status)
VALUES
    ('bar.order_item.void', 'Void or return a bar order item (high-risk, deducts bill and may restock)', 'bar', 'high', TRUE, 'active'),
    ('bar.bill.reopen', 'Re-open a settled bar bill (high-risk, supervisor review)', 'bar', 'high', TRUE, 'active'),
    ('bar.product.sold_out', 'Mark or restore a bar product as sold out (86)', 'bar', 'medium', FALSE, 'active'),
    ('bar.bill.tip', 'Add a tip to a bar bill', 'bar', 'medium', FALSE, 'active')
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
        'bar.order_item.void',
        'bar.bill.reopen',
        'bar.product.sold_out',
        'bar.bill.tip'
 )
ON CONFLICT (role_id, permission_code) DO NOTHING;
