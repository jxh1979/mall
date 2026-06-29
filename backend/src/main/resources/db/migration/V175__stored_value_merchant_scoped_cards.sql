-- B1d⑤ 储值卡分级：综合体通用卡 vs 商户级卡（用户 2026-06-28 裁决）。
-- 综合体通用卡：merchant_id IS NULL，综合体内各商户通用（沿用既有语义，按 project_id 隔离）。
-- 商户级卡：merchant_id 非空=发卡商户，只能在该商户消费（按 merchant 隔离）。
-- 一个会员在每个 project（通用卡）/每个 merchant（商户卡）各至多一张卡。
ALTER TABLE customer_stored_value_accounts
    ADD COLUMN IF NOT EXISTS merchant_id VARCHAR(64);

-- 原唯一约束 (customer_id, project_id) 无法容纳同一 project 下的多张商户卡 → 改为含 merchant_id 的范围唯一。
-- COALESCE 占位串使 NULL（通用卡无 merchant / 商户卡可无独立 project）按确定槽位参与唯一，避免 NULL 互不相等导致重复行。
ALTER TABLE customer_stored_value_accounts
    DROP CONSTRAINT IF EXISTS uq_csva_customer_project;
CREATE UNIQUE INDEX IF NOT EXISTS uq_csva_customer_scope
    ON customer_stored_value_accounts (
        customer_id,
        COALESCE(project_id, '__none__'),
        COALESCE(merchant_id, '__none__')
    );
CREATE INDEX IF NOT EXISTS idx_csva_merchant
    ON customer_stored_value_accounts (merchant_id, customer_id)
    WHERE merchant_id IS NOT NULL;
