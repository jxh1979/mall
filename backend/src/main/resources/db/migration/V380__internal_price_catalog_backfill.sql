-- V380 一价四存收编进内部结算 SKU 目录（批 10/E，用户拍板「全部收编成引用 + 业务表保留快照列」）：
--   ① 四张业务表各加目录引用列 catalog_item_code（引用 internal_price_catalog.item_code，非行 id）；
--   ② 存量内部价逐行迁成目录生效条目（is_draft=FALSE，价格逐分照抄 → 收编等价：迁移前后解析结果一致）；
--   ③ 原价格列自此转为【快照语义·只读】：目录是唯一可编辑处，写入口全部下线；
--      快照由系统在「设引用时」与「目录草案转正时」自动同步（见 JdbcSettlementConfigWriteRepository 传播 UPDATE），
--      任何人不得手改。流水级固化（ticket_verifications.settlement_price_cent / voucher_redemptions.settlement_amount_cent
--      / 结算账单各行）沿用既有范式不动 —— 已发生的账不受后续改价影响。
-- item_code 确定性生成（sku_bp_/sku_bc_/sku_et_/sku_vd_ + 源行 id）：幂等可重跑（WHERE catalog_item_code IS NULL 守卫），
--   收编等价测试按同 SQL 重放验证逐分不变。
-- ⚠️ .160 开发库数据最后全删：不做旧口径兼容/退化设计，直接按目标形态收编。

-- ① 引用列（不做 FK：item_code 跨行版本非唯一，归属校验在应用层「条目商家=业务对象提供商家」）。
ALTER TABLE bar_products         ADD COLUMN IF NOT EXISTS catalog_item_code VARCHAR(80);
ALTER TABLE bar_combos           ADD COLUMN IF NOT EXISTS catalog_item_code VARCHAR(80);
ALTER TABLE entitlement_templates ADD COLUMN IF NOT EXISTS catalog_item_code VARCHAR(80);
ALTER TABLE voucher_definitions  ADD COLUMN IF NOT EXISTS catalog_item_code VARCHAR(80);

CREATE INDEX IF NOT EXISTS idx_bar_products_catalog_ref ON bar_products (catalog_item_code)
    WHERE catalog_item_code IS NOT NULL;
CREATE INDEX IF NOT EXISTS idx_bar_combos_catalog_ref ON bar_combos (catalog_item_code)
    WHERE catalog_item_code IS NOT NULL;
CREATE INDEX IF NOT EXISTS idx_entitlement_templates_catalog_ref ON entitlement_templates (catalog_item_code)
    WHERE catalog_item_code IS NOT NULL;
CREATE INDEX IF NOT EXISTS idx_voucher_definitions_catalog_ref ON voucher_definitions (catalog_item_code)
    WHERE catalog_item_code IS NOT NULL;

-- ② 存量收编（四处 → 目录生效条目 + 回写引用）。
--    只收「有价」的行（价格列非空）；目录条目必须绑商家，商家取各表的提供方商户列
--    （权益模板取 COALESCE(merchant_id, exclusive_merchant_id) = 结算判定的提供方口径；两者皆空的存量模板
--    无从绑定商家、不入目录，其快照列维持原值、解析行为照旧 fail-closed/回退 value_amount）。
--    商家须真实存在（FK 兜底：LEFT JOIN merchants 过滤悬挂引用的脏数据行，不让整个迁移炸掉）。

-- ②a 酒吧商品（V152）：品类沿用商品粗类别 8 词表（bar_products 的粗类别落在 product_type 列，I-2 词表校验同源）。
INSERT INTO internal_price_catalog (id, item_code, merchant_id, category, item_name, price_cent,
                                    status, is_draft, updated_by, created_at, updated_at)
SELECT 'sku_bp_' || p.id, 'sku_bp_' || p.id, p.bar_id, p.product_type, p.product_name,
       p.internal_settlement_price_cent, 'active', FALSE, 'migration_v380', now(), now()
  FROM bar_products p
  JOIN merchants m ON m.id = p.bar_id
 WHERE p.internal_settlement_price_cent IS NOT NULL
   AND p.catalog_item_code IS NULL
ON CONFLICT DO NOTHING;

UPDATE bar_products p
   SET catalog_item_code = 'sku_bp_' || p.id
  FROM merchants m
 WHERE m.id = p.bar_id
   AND p.internal_settlement_price_cent IS NOT NULL
   AND p.catalog_item_code IS NULL;

-- ②b 酒吧套餐（V257）：品类固定 combo（套餐）。
INSERT INTO internal_price_catalog (id, item_code, merchant_id, category, item_name, price_cent,
                                    status, is_draft, updated_by, created_at, updated_at)
SELECT 'sku_bc_' || c.id, 'sku_bc_' || c.id, c.bar_id, 'combo', c.combo_name,
       c.internal_settlement_price_cent, 'active', FALSE, 'migration_v380', now(), now()
  FROM bar_combos c
  JOIN merchants m ON m.id = c.bar_id
 WHERE c.internal_settlement_price_cent IS NOT NULL
   AND c.catalog_item_code IS NULL
ON CONFLICT DO NOTHING;

UPDATE bar_combos c
   SET catalog_item_code = 'sku_bc_' || c.id
  FROM merchants m
 WHERE m.id = c.bar_id
   AND c.internal_settlement_price_cent IS NOT NULL
   AND c.catalog_item_code IS NULL;

-- ②c 票务权益模板（V225 签约价 = 虚拟权益内部结算价）：品类沿用权益类型。
INSERT INTO internal_price_catalog (id, item_code, merchant_id, category, item_name, price_cent,
                                    status, is_draft, updated_by, created_at, updated_at)
SELECT 'sku_et_' || t.id, 'sku_et_' || t.id, COALESCE(t.merchant_id, t.exclusive_merchant_id),
       t.entitlement_type, t.name, t.agreement_price_cent, 'active', FALSE, 'migration_v380', now(), now()
  FROM entitlement_templates t
  JOIN merchants m ON m.id = COALESCE(t.merchant_id, t.exclusive_merchant_id)
 WHERE t.agreement_price_cent IS NOT NULL
   AND t.catalog_item_code IS NULL
ON CONFLICT DO NOTHING;

UPDATE entitlement_templates t
   SET catalog_item_code = 'sku_et_' || t.id
  FROM merchants m
 WHERE m.id = COALESCE(t.merchant_id, t.exclusive_merchant_id)
   AND t.agreement_price_cent IS NOT NULL
   AND t.catalog_item_code IS NULL;

-- ②d 券定义（V301）：品类沿用券种（in_store_goods 等）。形状约束保证 internal_price 模式必有 merchant_id。
INSERT INTO internal_price_catalog (id, item_code, merchant_id, category, item_name, price_cent,
                                    status, is_draft, updated_by, created_at, updated_at)
SELECT 'sku_vd_' || v.id, 'sku_vd_' || v.id, v.merchant_id, v.voucher_kind, v.name,
       v.internal_price_cent, 'active', FALSE, 'migration_v380', now(), now()
  FROM voucher_definitions v
  JOIN merchants m ON m.id = v.merchant_id
 WHERE v.internal_price_cent IS NOT NULL
   AND v.catalog_item_code IS NULL
ON CONFLICT DO NOTHING;

UPDATE voucher_definitions v
   SET catalog_item_code = 'sku_vd_' || v.id
  FROM merchants m
 WHERE m.id = v.merchant_id
   AND v.internal_price_cent IS NOT NULL
   AND v.catalog_item_code IS NULL;

-- ③ 快照语义列注释（唯一可编辑处 = 目录；这些列自此只由系统写）。
COMMENT ON COLUMN bar_products.internal_settlement_price_cent IS
    'V380 起为目录价快照（只读）：唯一可编辑处 = internal_price_catalog（草案→他人复核转正）；系统在设引用/目录转正时自动同步，任何人不得手改。';
COMMENT ON COLUMN bar_combos.internal_settlement_price_cent IS
    'V380 起为目录价快照（只读）：唯一可编辑处 = internal_price_catalog；系统自动同步，任何人不得手改。';
COMMENT ON COLUMN entitlement_templates.agreement_price_cent IS
    'V380 起为目录价快照（只读）：唯一可编辑处 = internal_price_catalog；系统自动同步，任何人不得手改（PR#274 模板改价端点随之下线）。';
COMMENT ON COLUMN voucher_definitions.internal_price_cent IS
    'V380 起为目录价快照（只读）：唯一可编辑处 = internal_price_catalog；系统自动同步，任何人不得手改。';
COMMENT ON COLUMN bar_products.catalog_item_code IS '内部结算 SKU 目录引用（internal_price_catalog.item_code）。';
COMMENT ON COLUMN bar_combos.catalog_item_code IS '内部结算 SKU 目录引用（internal_price_catalog.item_code）。';
COMMENT ON COLUMN entitlement_templates.catalog_item_code IS '内部结算 SKU 目录引用（internal_price_catalog.item_code）。';
COMMENT ON COLUMN voucher_definitions.catalog_item_code IS '内部结算 SKU 目录引用（internal_price_catalog.item_code）。';
