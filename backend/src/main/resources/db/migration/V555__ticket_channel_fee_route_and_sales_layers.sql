-- 文件版本：V0.01
-- V555：通票分账渠道费分两层——通路费与销售渠道费（第十八批第 3 条）。
-- 依据：BSM L749「渠道费用分两层、性质不同不可混用：通路费（银行、微信、支付宝等支付通路收取）所有支付都会产生、官方渠道同样产生；
--   销售渠道费（第三方售票平台佣金）只有经第三方渠道售出才产生，在通路费之上再扣一层。两者一律在拆分给各域之前从整单扣除且只扣一次，
--   由各域按分配份额自然共担……官方渠道售出的票不扣销售渠道费，但仍扣通路费……每笔分摊须记录所用合同版本」；
--   ENUM L52 第一步「从整单扣一次通路费，经第三方渠道售出的再从整单扣一次销售渠道费」；
--   ENUM L86「储值卡充值时即产生通路费，按该笔充值实际使用的支付通路记录金额与费率」。
-- 铁律 8：不回填存量——存量配置行一律视为销售渠道费层；存量付款没有通路费固化行。

-- 1) 渠道费配置分层：route＝通路费（channel＝支付通路码，只按比例）；sales＝销售渠道费（channel＝第三方销售渠道码，留空＝第三方通用）。
ALTER TABLE channel_fee_configs
    ADD COLUMN fee_layer VARCHAR(16) NOT NULL DEFAULT 'sales',
    ADD CONSTRAINT chk_cfc_fee_layer CHECK (fee_layer IN ('route', 'sales')),
    ADD CONSTRAINT chk_cfc_route_by_rate CHECK (fee_layer = 'sales' OR (fixed_amount_cent IS NULL AND channel IS NOT NULL));
DROP INDEX IF EXISTS uq_cfc;
CREATE UNIQUE INDEX uq_cfc ON channel_fee_configs
    (COALESCE(project_id, '__DEFAULT__'), fee_layer, COALESCE(channel, '__ALL__'), is_draft) WHERE status = 'active';

-- 2) 票务订单每笔付款成功时按实际支付通路固化的通路费（一笔付款一行，之后改费率不回改）。
--    储值笔：通路费在充值时已产生，取该笔消费在各充值批次分摊的通路费之和（明细见 stored_value_consume_allocations，fee_bps/config_id 为空）。
CREATE TABLE ticket_payment_route_fees (
    payment_intent_id VARCHAR(64) PRIMARY KEY REFERENCES ticket_payment_intents(id),
    order_id          VARCHAR(64) NOT NULL,
    project_id        VARCHAR(64),
    payment_channel   VARCHAR(64) NOT NULL,
    amount_cent       BIGINT NOT NULL,
    fee_bps           INTEGER,
    fee_cent          BIGINT NOT NULL,
    config_id         VARCHAR(48),
    fixed_at          TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT chk_tprf_amount CHECK (amount_cent > 0 AND fee_cent >= 0)
);
CREATE INDEX idx_tprf_order ON ticket_payment_route_fees (order_id);
COMMENT ON TABLE ticket_payment_route_fees IS '票务付款成功时按支付通路固化的通路费（BSM L749 / ENUM L52 / ENUM L86）';

-- 2b) 票务订单的渠道费固化（一单一行）：首笔受理付款时按订单销售渠道固化销售渠道费所用配置（比例或每张票固定金额；之后改配置不回改，
--     官方渠道不扣、不建销售渠道费字段）；route_allocated_part_cent＝本单已分摊通路费的部分合计——各部分按累计额差分分摊，
--     本单各部分分摊之和恰为固化的通路费，不产生尾差。
CREATE TABLE ticket_order_channel_fees (
    order_id                  VARCHAR(64) PRIMARY KEY,
    project_id                VARCHAR(64),
    sales_channel             VARCHAR(64),
    sales_fee_config_id       VARCHAR(48),
    sales_fee_bps             INTEGER,
    sales_fixed_amount_cent   BIGINT,
    sales_fixed_at            TIMESTAMPTZ,
    route_allocated_part_cent BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT chk_tocf_sales CHECK ((sales_fee_config_id IS NULL) = (sales_fixed_at IS NULL)
        AND (sales_fee_config_id IS NULL OR sales_fee_bps IS NOT NULL))
);
COMMENT ON TABLE ticket_order_channel_fees IS '票务订单付款时固化的销售渠道费配置与通路费分摊进度（BSM L749「从整单扣除且只扣一次……每笔分摊须记录所用合同版本」）';

-- 3) 通路费单列：核销/到期事实逐笔分摊、结算单与业态份额各一列；channel_fee_cent 自此只指销售渠道费。
ALTER TABLE settlement_transactions ADD COLUMN route_fee_cent BIGINT NOT NULL DEFAULT 0;
ALTER TABLE settlement_bills ADD COLUMN route_fee_cent BIGINT NOT NULL DEFAULT 0;
ALTER TABLE settlement_bill_format_shares ADD COLUMN route_fee_cent BIGINT NOT NULL DEFAULT 0;

DO $$
BEGIN
    IF EXISTS (SELECT 1 FROM pg_roles WHERE rolname = 'complex_app') THEN
        EXECUTE 'GRANT SELECT, INSERT, UPDATE, DELETE ON ticket_payment_route_fees TO complex_app';
        EXECUTE 'GRANT SELECT, INSERT, UPDATE, DELETE ON ticket_order_channel_fees TO complex_app';
    END IF;
END $$;
