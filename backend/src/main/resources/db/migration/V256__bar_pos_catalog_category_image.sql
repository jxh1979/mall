-- V256 图形化点单 POS 阶段1（分类可配置 + 商品图片）：
-- 背景：平板图形化点单需要「可配置分类」（现有 bar_products.product_type 仅一个粗字符串，运营无法自助增删分类，
--   POS 侧无法按分类分屏列菜）与「商品图片」（现有商品目录无图，点单界面需缩略图）。业主 2026-07-06 拍板：
--   ① 分类可配置——后台自建分类表，商品挂 category_id（不写死枚举）；② 图片存服务器本地——沿用 image_url 先例
--   （见 mall_floor_plans.image_url / floor_plan：image_url 存相对路径字符串，不上云）。
-- 口径：
--   bar_product_categories：每个吧台（bar_id）自维护一套分类；名称 category_name；sort_order 排序（POS 分屏顺序）；
--     status active/archived（archived=停用，不在 POS 出现，但历史商品仍可引用）；merchant_id 租户隔离（同 bar_products）。
--   bar_products.category_id：可空 FK → bar_product_categories(id)。可空=向后兼容既有商品（未分类）；POS 拉菜单按此归组。
--     注意：product_type（粗字符串，历史）保留不动，两者并存——category_id 是可配置精分类，product_type 是既有兼容字段。
--   bar_products.image_url：可空，商品图片相对路径（同 floor_plan image_url 语义：本地存储路径字符串，非二进制）。

CREATE TABLE IF NOT EXISTS bar_product_categories (
    id VARCHAR(64) PRIMARY KEY,
    bar_id VARCHAR(64) NOT NULL,
    merchant_id VARCHAR(100),
    category_name VARCHAR(128) NOT NULL,
    sort_order INTEGER NOT NULL DEFAULT 0,
    status VARCHAR(32) NOT NULL DEFAULT 'active',
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,
    CONSTRAINT chk_bar_product_categories_status CHECK (status IN ('active', 'archived'))
);

CREATE INDEX IF NOT EXISTS idx_bar_product_categories_bar
    ON bar_product_categories (bar_id, status, sort_order);
CREATE INDEX IF NOT EXISTS idx_bar_product_categories_merchant
    ON bar_product_categories (merchant_id) WHERE merchant_id IS NOT NULL;

ALTER TABLE bar_products
    ADD COLUMN IF NOT EXISTS category_id VARCHAR(64),
    ADD COLUMN IF NOT EXISTS image_url VARCHAR(1024);

ALTER TABLE bar_products
    DROP CONSTRAINT IF EXISTS fk_bar_products_category;
ALTER TABLE bar_products
    ADD CONSTRAINT fk_bar_products_category
    FOREIGN KEY (category_id) REFERENCES bar_product_categories(id);

CREATE INDEX IF NOT EXISTS idx_bar_products_category
    ON bar_products (bar_id, category_id) WHERE category_id IS NOT NULL;
