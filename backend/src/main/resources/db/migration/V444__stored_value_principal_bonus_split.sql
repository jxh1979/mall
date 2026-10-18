-- CG-06a 储值本金 / 赠送金两段记账。
-- 依据：ENUM「储值卡类型与收款主体」：「每笔充值拆为本金与赠送金两段分别记账，赠送比例可配（业务参数，按项目可配，须同时配置上下限）；
--       消费扣款顺序写死为先扣本金、后扣赠送金，不可配置（仅商户级卡例外，其扣款顺序由发卡商户自定、由平台运营代为录入后台）」；
--       BSM「我的余额」：「含本金与赠送金分列……每笔充值（时间、金额、本金与赠送金拆分、支付通路）、每笔扣款（商户、扣减的本金与赠送金各多少）」；
--       BSM「支付方式」：「储值卡（通用卡先扣本金后扣赠送金且不可配置；商户级卡消费扣款顺序由发卡商户自定、由平台运营代为录入后台）」。
-- 铁律 8：不为存量数据回填；存量账户两段均为 0，库里数据最后都要删光。

-- 1) 账户两段余额（balance_cent 仍为两段之和，由服务层维护）
ALTER TABLE customer_stored_value_accounts
    ADD COLUMN IF NOT EXISTS principal_cent BIGINT NOT NULL DEFAULT 0,
    ADD COLUMN IF NOT EXISTS bonus_cent BIGINT NOT NULL DEFAULT 0;
ALTER TABLE customer_stored_value_accounts
    DROP CONSTRAINT IF EXISTS chk_csva_principal_nonneg,
    DROP CONSTRAINT IF EXISTS chk_csva_bonus_nonneg;
ALTER TABLE customer_stored_value_accounts
    ADD CONSTRAINT chk_csva_principal_nonneg CHECK (principal_cent >= 0),
    ADD CONSTRAINT chk_csva_bonus_nonneg CHECK (bonus_cent >= 0);
COMMENT ON COLUMN customer_stored_value_accounts.principal_cent IS '未消费本金（分）：客人实付充值累计 − 本金扣减 − 本金退还';
COMMENT ON COLUMN customer_stored_value_accounts.bonus_cent IS '未使用赠送金（分）：充值按项目赠送比例形成，不退现，销户作废';

-- 2) 流水两段拆分（amount_cent = principal_cent + bonus_cent，均带方向）
ALTER TABLE customer_stored_value_transactions
    ADD COLUMN IF NOT EXISTS principal_cent BIGINT NOT NULL DEFAULT 0,
    ADD COLUMN IF NOT EXISTS bonus_cent BIGINT NOT NULL DEFAULT 0;
COMMENT ON COLUMN customer_stored_value_transactions.principal_cent IS '本笔变动中的本金部分（分，带方向）';
COMMENT ON COLUMN customer_stored_value_transactions.bonus_cent IS '本笔变动中的赠送金部分（分，带方向）';

-- 3) 商户级卡消费扣款顺序（由发卡商户自定、平台运营代录）；通用卡写死先本金后赠送金，不在此表
ALTER TABLE stored_value_merchant_refund_rules
    ADD COLUMN IF NOT EXISTS consume_order VARCHAR(16) NOT NULL DEFAULT 'principal_first';
ALTER TABLE stored_value_merchant_refund_rules
    DROP CONSTRAINT IF EXISTS chk_svmrr_consume_order;
ALTER TABLE stored_value_merchant_refund_rules
    ADD CONSTRAINT chk_svmrr_consume_order CHECK (consume_order IN ('principal_first', 'bonus_first'));
COMMENT ON COLUMN stored_value_merchant_refund_rules.consume_order IS '商户级卡消费扣款顺序：principal_first=先本金后赠送金；bonus_first=先赠送金后本金';

-- 4) 通用卡充值赠送比例（业务参数，按项目可配，须同时配置上下限；未配置=不赠送）
CREATE TABLE IF NOT EXISTS stored_value_bonus_policies (
    project_id  VARCHAR(64) PRIMARY KEY,   -- 综合体项目 id；'__global__' = 无项目范围的通用卡
    bonus_bps   INTEGER NOT NULL,          -- 赠送比例（万分比）：赠送金 = 实付本金 × bonus_bps / 10000
    min_bps     INTEGER NOT NULL,          -- 已配置下限（万分比）
    max_bps     INTEGER NOT NULL,          -- 已配置上限（万分比）
    updated_by  VARCHAR(64),
    updated_at  TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT chk_svbp_bounds CHECK (min_bps >= 0 AND max_bps <= 10000 AND min_bps <= bonus_bps AND bonus_bps <= max_bps)
);
COMMENT ON TABLE stored_value_bonus_policies IS '综合体通用卡充值赠送比例（按项目可配，值与上下限同在；ENUM「储值卡类型与收款主体」）';
