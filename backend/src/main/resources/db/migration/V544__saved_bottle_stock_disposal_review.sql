-- 文件版本：V0.01
-- V544：OBS-13-02 存酒到期转库存后的处置审批（第十四批）。
-- 依据：BSM L1072「转库存之后的报废、赠送、内部使用等处置必须走审批并保留操作记录」；操作员裁决补充（第十四批）第 4 条
--   「门店主管与财务审批」：门店主管和财务都批（同「退出前无法交还的赔付方案」那样）。
-- 到期转库存本身（回库流水 movement_type=return、business_type=saved_bottle_to_stock）不需要改表。不回填存量数据。

-- ① 库存域复核单新增复核对象「存酒库房存货处置」，并记发起人与两道审批人（审批人不得是发起人、两道不得同一人，同赔付方案审批）。
ALTER TABLE inventory_high_value_reviews DROP CONSTRAINT IF EXISTS chk_inv_hv_subject;
ALTER TABLE inventory_high_value_reviews ADD CONSTRAINT chk_inv_hv_subject
    CHECK (subject_type IN ('damage_report', 'stock_variance', 'saved_bottle_void', 'saved_bottle_stock_disposal'));
ALTER TABLE inventory_high_value_reviews
    ADD COLUMN IF NOT EXISTS created_by VARCHAR(64),
    ADD COLUMN IF NOT EXISTS supervisor_approved_by VARCHAR(64),
    ADD COLUMN IF NOT EXISTS finance_approved_by VARCHAR(64);

-- ② 存酒库房的出库（赠送、内部使用等走 outbound；积分兑换走 redeem_consume）先落待审调整单，两方批准后才扣库存并写同类型流水。
ALTER TABLE inventory_stock_adjustments DROP CONSTRAINT IF EXISTS chk_stock_adjustment_type;
ALTER TABLE inventory_stock_adjustments ADD CONSTRAINT chk_stock_adjustment_type
    CHECK (adjustment_type IN ('gain', 'loss', 'damage', 'waste', 'count_correction', 'outbound', 'redeem_consume'))
    NOT VALID;
