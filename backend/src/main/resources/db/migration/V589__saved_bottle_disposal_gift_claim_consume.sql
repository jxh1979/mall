-- 文件版本：V0.01
-- V589（第十八批 M 段第 112 条 112-R3 配套）：存酒库房处置待审调整单的类型闭集加「礼物领取消耗」。
-- 依据：BSM:1072「转库存之后的报废、赠送、内部使用等处置必须走审批并保留操作记录」；第 112 条 112-R3（BSM:1194「积分兑换消耗、礼物领取消耗
--   （三种“消耗”分开记）」）：营销赠发的店内实物券与礼物领取改记 gift_claim_consume，库位为存酒库房时同样先落待审调整单（V544）。
-- 只在 V544 原闭集上加 'gift_claim_consume'，不回填存量数据。

ALTER TABLE inventory_stock_adjustments DROP CONSTRAINT IF EXISTS chk_stock_adjustment_type;
ALTER TABLE inventory_stock_adjustments ADD CONSTRAINT chk_stock_adjustment_type
    CHECK (adjustment_type IN ('gain', 'loss', 'damage', 'waste', 'count_correction', 'outbound', 'redeem_consume', 'gift_claim_consume'))
    NOT VALID;
