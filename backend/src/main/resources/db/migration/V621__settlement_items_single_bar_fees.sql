-- 文件版本：V0.01
-- V621（第十八批 AI 段第 212 条 212-R1）：单一酒吧门票的通路费、销售渠道费从该酒吧直付额先扣，按商户在专属直付明细上留痕。
-- 依据：ENUM:51「所有支付都会产生通路费、官方渠道同样产生……不由该笔业务的收款主体单独承担」「任何票种经第三方渠道售出时，销售渠道费一律先从整单扣除
--   且只扣一次」「每笔分摊的结果与所用合同版本必须留痕」；BSM:749「官方渠道售出的票不扣销售渠道费，但仍扣通路费」。
-- 所用合同版本即订单付款时固化的销售渠道费配置（ticket_order_channel_fees）；池分账明细与追收明细恒 0。

ALTER TABLE settlement_items
    ADD COLUMN single_bar_route_fee_cent BIGINT NOT NULL DEFAULT 0,
    ADD COLUMN single_bar_channel_fee_cent BIGINT NOT NULL DEFAULT 0;

COMMENT ON COLUMN settlement_items.single_bar_route_fee_cent IS
    '该商户直付额中已扣的单一酒吧门票通路费（分；ENUM:51「每笔分摊的结果与所用合同版本必须留痕」）';
COMMENT ON COLUMN settlement_items.single_bar_channel_fee_cent IS
    '该商户直付额中已扣的单一酒吧门票销售渠道费（分；按订单付款时固化的销售渠道费配置 ticket_order_channel_fees 计）';
