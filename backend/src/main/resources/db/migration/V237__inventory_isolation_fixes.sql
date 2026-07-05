-- 库存域越权隔离修复（审计定位 C2/C3）。
-- 基线 origin/main=170acb0；当前最高迁移 V235，财务占用 V236，本迁移取 V237。
--
--   C2 项目级读隔离：inventory_stock_balances 增 project_id 列（写入时由服务层从 currentProjectScopeQuietly 落库），
--      供项目员工只读本项目余额、不越读他项目/全平台库存。历史行 project_id 保持 NULL（NULL=不限项目，与既有
--      merchant_id NULL 放行语义一致，避免历史库存对既有平台/商户读通道产生回归）。
--   C3 高价值复核归属：inventory_high_value_reviews 增 merchant_id 列 + 从 subject 回填，
--      使 reviewInternal 可据此拒绝跨商户翻转他人复核单（此前 saved_bottle_void reject 分支无任何 scope 校验）。

-- ── C2：余额行项目归属列 ──────────────────────────────────────────────────────
ALTER TABLE inventory_stock_balances ADD COLUMN IF NOT EXISTS project_id VARCHAR(64);

CREATE INDEX IF NOT EXISTS idx_inventory_stock_balances_project
    ON inventory_stock_balances (project_id)
 WHERE project_id IS NOT NULL;

-- ── C3：高价值复核单归属商户列 + 从 subject 回填 ──────────────────────────────
ALTER TABLE inventory_high_value_reviews ADD COLUMN IF NOT EXISTS merchant_id VARCHAR(64);

CREATE INDEX IF NOT EXISTS idx_inventory_high_value_reviews_merchant
    ON inventory_high_value_reviews (merchant_id)
 WHERE merchant_id IS NOT NULL;

-- 回填：存酒作废单归属 = saved_bottles.merchant_id。
UPDATE inventory_high_value_reviews r
   SET merchant_id = b.merchant_id
  FROM saved_bottles b
 WHERE r.subject_type = 'saved_bottle_void'
   AND r.subject_id = b.id
   AND r.merchant_id IS NULL;

-- 回填：报损/盘点差异单归属 = inventory_stock_adjustments.merchant_id。
UPDATE inventory_high_value_reviews r
   SET merchant_id = a.merchant_id
  FROM inventory_stock_adjustments a
 WHERE r.subject_type IN ('damage_report', 'stock_variance')
   AND r.subject_id = a.id
   AND r.merchant_id IS NULL;
