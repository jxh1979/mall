-- P6 综合服务费守恒架构改(2026-07-21):把每笔核销「实收综合服务费」落为不可变事实,红冲严格取原行之反。
-- 背景:原实现在 settleMonth 月末按【聚合 gross】重算 fee(固定额 min(fixed×count,gross) / 封顶 min(raw,cap)),
--   这两个 clamp 是【月度非线性】,不满足核销↔冲正的带符号线性可加(f(-x)≠-f(x))。过账开启后,原核销月已过账时
--   其跨月红冲落【当月】、聚合行 gross 带负,按费率/毛额重算 fee ≠ 原月实收之反 → 触顶/固定额场景项目倒亏、商户多拿,
--   net<0 还会撞 chk_si_amount 致 settleMonth 崩(对抗审计 2026-07-21 坐实,过账激活前必修)。
-- 修法:核销时(SettlementSettledConsumer)按【每笔·增量封顶】算实收 fee 落 comp_service_fee_cent;冲正时
--   (SettlementReversedConsumer)取原 ST-VS 行 fee 之反落负值;settleMonth 改为【线性求和】各行 comp_service_fee_cent
--   替代月末重算 → 固定额/封顶/比例三模式跨月红冲全守恒(每笔实收精确对冲)。
-- comp_service_fee_config_id:该行解析到的费率规则主键,供增量封顶按「商户×规则」查月累计(月内同规则 Σfee ≤ cap)。
-- 均无 ≥0 约束(冲正行 fee 为负,同 discount_amount_cent 语义);默认 0 = 无专属/无费率 → 零行为变更。
ALTER TABLE settlement_transactions
    ADD COLUMN IF NOT EXISTS comp_service_fee_cent      BIGINT NOT NULL DEFAULT 0,
    ADD COLUMN IF NOT EXISTS comp_service_fee_config_id VARCHAR(64);

COMMENT ON COLUMN settlement_transactions.comp_service_fee_cent IS
    'P6:本笔核销/冲正的实收综合服务费(核销为正·冲正取原行之反为负);settleMonth 线性求和替代月末重算,保跨月红冲守恒';
COMMENT ON COLUMN settlement_transactions.comp_service_fee_config_id IS
    'P6:本笔 fee 解析到的综合服务费费率规则 id;供增量封顶按「商户×规则」查月内累计';

-- 增量封顶月累计查询走 (exclusive_merchant_id, comp_service_fee_config_id, business_at) → 建部分索引加速。
CREATE INDEX IF NOT EXISTS idx_st_svcfee_mtd
    ON settlement_transactions (exclusive_merchant_id, comp_service_fee_config_id, business_at)
    WHERE comp_service_fee_config_id IS NOT NULL;
