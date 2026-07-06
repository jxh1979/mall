-- V257 图形化点单 POS 阶段1（套餐 combo）：
-- 背景：POS 需支持「套餐」——运营在后台从既有 SKU（bar_products）挑若干件组成套餐，定套餐整体售价。业主 2026-07-06 拍板：
--   套餐后台填（选包含哪些 SKU + 数量）；套餐有独立售价 price_cent 与内部结算价 internal_settlement_price_cent
--   （语义同 bar_products：平台↔酒吧结算价基，可空），有图片 image_url（同 bar_products 本地存储先例）。
-- 口径：
--   bar_combos：每个吧台一套；combo_name 名称；price_cent 套餐售价（分，>=0）；
--     internal_settlement_price_cent 内部结算价（分，可空，非空 >=0）；image_url 图片相对路径可空；
--     status active/archived；merchant_id 租户隔离。
--   bar_combo_items：套餐→含哪些 bar_product 及数量。combo_id FK（删套餐级联删明细）；product_id FK → bar_products；
--     quantity >0。同一套餐同一 SKU 唯一（(combo_id, product_id) 唯一，改数量走 UPDATE，不重复插）。
-- 阶段1 只做目录建模；下单时套餐如何展开为 bar_order_items / 结算按内部价拆分，留阶段2（POS 下单）实现。

CREATE TABLE IF NOT EXISTS bar_combos (
    id VARCHAR(64) PRIMARY KEY,
    bar_id VARCHAR(64) NOT NULL,
    merchant_id VARCHAR(100),
    combo_name VARCHAR(128) NOT NULL,
    price_cent BIGINT NOT NULL,
    internal_settlement_price_cent BIGINT,
    image_url VARCHAR(1024),
    status VARCHAR(32) NOT NULL DEFAULT 'active',
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,
    CONSTRAINT chk_bar_combos_price CHECK (price_cent >= 0),
    CONSTRAINT chk_bar_combos_internal_price
        CHECK (internal_settlement_price_cent IS NULL OR internal_settlement_price_cent >= 0),
    CONSTRAINT chk_bar_combos_status CHECK (status IN ('active', 'archived'))
);

CREATE INDEX IF NOT EXISTS idx_bar_combos_bar
    ON bar_combos (bar_id, status);
CREATE INDEX IF NOT EXISTS idx_bar_combos_merchant
    ON bar_combos (merchant_id) WHERE merchant_id IS NOT NULL;

CREATE TABLE IF NOT EXISTS bar_combo_items (
    id VARCHAR(64) PRIMARY KEY,
    combo_id VARCHAR(64) NOT NULL REFERENCES bar_combos(id) ON DELETE CASCADE,
    product_id VARCHAR(64) NOT NULL REFERENCES bar_products(id),
    quantity INTEGER NOT NULL,
    created_at TIMESTAMPTZ NOT NULL,
    CONSTRAINT chk_bar_combo_items_quantity CHECK (quantity > 0),
    CONSTRAINT uq_bar_combo_items_product UNIQUE (combo_id, product_id)
);

CREATE INDEX IF NOT EXISTS idx_bar_combo_items_combo
    ON bar_combo_items (combo_id);
