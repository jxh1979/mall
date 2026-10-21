-- 文件版本：V0.01
-- V528：TK-04-b 商户消费积分等待期届满释放、对应退款门槛与冲回、商户积分发行费并入积分月账单（第十一批）。
-- 依据：BSM L823「冻结与有效消费」（等待期已满且无待处理的对应退款申请才转为可用；部分退款只取消对应尚待确认积分，剩余仍按原完成时点；
--   退款待审、已批待退、处理中、失败仍需办理时继续待确认、不产生商户积分发行费；已可用积分后来退款按原追回及待追回积分规则处理；
--   只有转为可用才产生商户积分发行费）；ENUM L72「基础积分和等级加成积分成本均由发生消费的商户承担」；ENUM L75「积分发行费＝转为可用的
--   积分数×全国统一积分发行单价」；ENUM L84 积分结算账期（自然月计费、次月出账、各自全额挂账）；台账 §31 Q38B。不回填存量数据。

-- ① 奖励快照增已退有效消费累计、已取消／扣回积分、已冲回成长值与确认事实（原锁定数值不改）。
ALTER TABLE merchant_consumption_rewards
    ADD COLUMN refunded_amount_cent BIGINT NOT NULL DEFAULT 0 CHECK (refunded_amount_cent >= 0),
    ADD COLUMN reversed_points BIGINT NOT NULL DEFAULT 0 CHECK (reversed_points >= 0),
    ADD COLUMN reversed_growth BIGINT NOT NULL DEFAULT 0 CHECK (reversed_growth >= 0),
    ADD COLUMN confirmed_at TIMESTAMPTZ,
    ADD COLUMN confirmed_points BIGINT NOT NULL DEFAULT 0 CHECK (confirmed_points >= 0),
    ADD CONSTRAINT chk_merchant_rewards_reversal_bounds CHECK (refunded_amount_cent <= effective_amount_cent
        AND reversed_points <= points AND reversed_growth <= growth AND confirmed_points <= points);

-- ② 同一笔商户消费可多次部分退款：成长值冲回逐次追加，获取仍一单一条。
ALTER TABLE customer_growth_ledger DROP CONSTRAINT uq_customer_growth_ledger_merchant;
CREATE UNIQUE INDEX uq_customer_growth_ledger_merchant_earn ON customer_growth_ledger (merchant_reward_id) WHERE change_type = 'earn';

-- ③ 待追回积分同时承接门票与商户消费两类来源，二者恰有其一；同一笔商户消费的待追回累计在一行。
ALTER TABLE customer_points_recoveries
    DROP CONSTRAINT customer_points_recoveries_pkey,
    ALTER COLUMN reward_id DROP NOT NULL,
    ADD COLUMN merchant_reward_id VARCHAR(64) REFERENCES merchant_consumption_rewards(id),
    ADD CONSTRAINT uq_customer_points_recoveries_reward UNIQUE (reward_id),
    ADD CONSTRAINT uq_customer_points_recoveries_merchant UNIQUE (merchant_reward_id),
    ADD CONSTRAINT chk_customer_points_recoveries_source CHECK (num_nonnulls(reward_id, merchant_reward_id) = 1);

-- ④ 商户积分发行费事实：转为可用时按转可用积分数×释放时全国发行单价计算，承担方为消费商户及其合同主体
--    （列名沿用积分月账单的承担方字段 seller_*），收款方为全国积分结算中心。
CREATE TABLE merchant_points_issuance_fees (
    reward_id VARCHAR(64) PRIMARY KEY REFERENCES merchant_consumption_rewards(id),
    price_version_id VARCHAR(64) NOT NULL REFERENCES ticket_reward_config_versions(id),
    seller_entity_id VARCHAR(64) NOT NULL REFERENCES business_entities(id),
    seller_merchant_id VARCHAR(64) NOT NULL REFERENCES merchants(id),
    receiver_entity_id VARCHAR(64) NOT NULL REFERENCES business_entities(id),
    project_id VARCHAR(64) NOT NULL,
    points BIGINT NOT NULL CHECK (points > 0),
    unit_price_cent NUMERIC(18,6) NOT NULL CHECK (unit_price_cent > 0),
    amount_cent BIGINT NOT NULL CHECK (amount_cent >= 0),
    confirmed_at TIMESTAMPTZ NOT NULL
);
CREATE TRIGGER merchant_points_fee_immutable BEFORE UPDATE OR DELETE ON merchant_points_issuance_fees
    FOR EACH ROW EXECUTE FUNCTION complex_block_mutation();
CREATE TRIGGER merchant_points_fee_month_closed BEFORE INSERT ON merchant_points_issuance_fees
    FOR EACH ROW EXECUTE FUNCTION ticket_points_fee_month_guard();

-- ⑤ 积分月账单按费用类别分账：门票积分发行费与商户积分发行费各自出账、各自挂账。
ALTER TABLE ticket_points_monthly_bills
    ADD COLUMN fee_kind VARCHAR(16) NOT NULL DEFAULT 'ticket' CHECK (fee_kind IN ('ticket', 'merchant'));
DROP INDEX ticket_points_bill_scope;
CREATE UNIQUE INDEX ticket_points_bill_scope ON ticket_points_monthly_bills
    (billing_month, fee_kind, seller_entity_id, COALESCE(seller_merchant_id, ''), receiver_entity_id, project_id);
CREATE TABLE merchant_points_monthly_bill_items (
    reward_id VARCHAR(64) PRIMARY KEY REFERENCES merchant_points_issuance_fees(reward_id),
    bill_id VARCHAR(64) NOT NULL REFERENCES ticket_points_monthly_bills(id)
);
CREATE TRIGGER merchant_points_bill_item_immutable BEFORE UPDATE OR DELETE ON merchant_points_monthly_bill_items
    FOR EACH ROW EXECUTE FUNCTION complex_block_mutation();

DO $$
BEGIN
    IF EXISTS (SELECT 1 FROM pg_roles WHERE rolname = 'complex_app') THEN
        EXECUTE 'GRANT SELECT, INSERT ON merchant_points_issuance_fees TO complex_app';
        EXECUTE 'GRANT SELECT, INSERT ON merchant_points_monthly_bill_items TO complex_app';
    END IF;
END $$;
