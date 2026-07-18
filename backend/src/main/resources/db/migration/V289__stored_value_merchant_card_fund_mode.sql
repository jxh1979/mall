-- AUD-058/059/060/061 补全：商户卡资金模式 + 发卡资格 + 卡级资金模式开卡定格。
-- 用户拍板口径：
--   1) 发卡资格：平台后台给商户开通「发卡资格」后客人才能给该商户卡充值（新增开关，默认关）。
--   2) 资金模式：商户预收(merchant_prepaid=商户自收) / 平台代收(platform_collect)，逐户配置，仅平台可改。
--   3) 一张卡=一种资金归属：开卡时把当时商户的资金模式定格到卡，后台改配置只影响之后新开的卡，已发卡不变。
--   4) 无历史回填。

-- 复用 V176 stored_value_merchant_refund_rules 承载商户卡配置（不另造平行表）：
ALTER TABLE stored_value_merchant_refund_rules
    ADD COLUMN IF NOT EXISTS card_issuance_enabled BOOLEAN     NOT NULL DEFAULT FALSE,
    ADD COLUMN IF NOT EXISTS fund_ownership        VARCHAR(32) NOT NULL DEFAULT 'merchant_prepaid';
ALTER TABLE stored_value_merchant_refund_rules
    ADD CONSTRAINT chk_svmrr_fund_ownership CHECK (fund_ownership IN ('merchant_prepaid','platform_collect'));

-- 卡级资金模式定格：开卡时钉进卡，终身不变。通用卡（merchant_id 空）恒 NULL。
-- 无历史回填：存量商户卡 fund_ownership 留 NULL；服务层读 NULL 视为「未定格的旧卡」→ 维持旧行为
-- （旧卡退卡按商户退款规则处理、不施加新预收退卡门控；旧卡消费不触发代收逐笔应付），仅新开卡受新口径约束。
ALTER TABLE customer_stored_value_accounts
    ADD COLUMN IF NOT EXISTS fund_ownership VARCHAR(32);
ALTER TABLE customer_stored_value_accounts
    ADD CONSTRAINT chk_csva_fund_ownership CHECK (
        fund_ownership IS NULL OR fund_ownership IN ('merchant_prepaid','platform_collect'));
