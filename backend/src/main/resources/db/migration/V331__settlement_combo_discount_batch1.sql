-- 结算配置中心 · 批1 · D6：套票折扣额走真实售卖/核销路径落库 + 结算按归属应用。
-- 依据 settlement-rebuild-prd §7.4(套票折扣归属 discount_bearer)+ 配置中心批1。
-- 背景:套票折扣额=Σ(各权益满额标准价) − 实付净额,定价期算得出但从未落库
--   (核销只记了折后净额,满额+折扣丢了),故 ticket_combo_discount_configs 有配置也空转。
-- 本迁移三处加列,均可空/带默认,可独立回滚;满额价为空(component_full_price_cent 全空)→折扣恒 0=零行为变更。

-- ① issued_entitlements:出票定价时记录该权益「满额份额」(各分项标准价按人拆的份额,分)。
--    可空:component_price 全空(回退均摊)→ NULL=无分项满额价=无套票折扣(与 allocated_revenue_cent 同 null 语义)。
--    核销结算时与 allocated 走同一 SettlementAmounts 增量公式算 verifiedFull,折扣增量=max(0, verifiedFull−verified)。
ALTER TABLE issued_entitlements
    ADD COLUMN IF NOT EXISTS component_full_price_cent BIGINT;

-- ② settlement_transactions:每笔核销/冲正的套票折扣额增量(满额−净额;分)。核销落正、冲正落负。
ALTER TABLE settlement_transactions
    ADD COLUMN IF NOT EXISTS discount_amount_cent BIGINT NOT NULL DEFAULT 0;

-- 扩展 V323 资金守恒 CHECK:把 discount_amount_cent 纳入正/负不变式,守 append-only 账本资金守恒——
--   非冲正行全部 >=0;冲正行全部 <=0(恰取原正向行的反)→ 任一 SUM 对「核销+冲正」配对天然净 0,无漂移。
--   既有行 discount_amount_cent 默认 0(0 同时满足两分支),重定义 CHECK 不违反存量、可独立回滚。
ALTER TABLE settlement_transactions DROP CONSTRAINT IF EXISTS chk_st_amount;
ALTER TABLE settlement_transactions ADD CONSTRAINT chk_st_amount CHECK (
    (transaction_type = 'verification_reversed'
        AND verified_amount_cent <= 0 AND unverified_amount_cent <= 0 AND allocated_revenue_cent <= 0
        AND discount_amount_cent <= 0)
    OR (transaction_type <> 'verification_reversed'
        AND verified_amount_cent >= 0 AND unverified_amount_cent >= 0 AND allocated_revenue_cent >= 0
        AND discount_amount_cent >= 0)
);

-- ③ settlement_bills:套票折扣入账——项目公司实际承担的折扣额(platform_absorb 全额/split_by_ratio 平台份额;
--    merchant_absorb=0)。记录商户拿到满额时项目为覆盖折扣多支出的金额。默认 0=零行为变更。
ALTER TABLE settlement_bills
    ADD COLUMN IF NOT EXISTS combo_discount_cent BIGINT NOT NULL DEFAULT 0;
ALTER TABLE settlement_bills DROP CONSTRAINT IF EXISTS chk_sb_combo_discount;
ALTER TABLE settlement_bills ADD CONSTRAINT chk_sb_combo_discount CHECK (combo_discount_cent >= 0);
