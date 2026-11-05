-- 文件版本：V0.01
-- V602（第十八批 T 段第 157 条 157-R6）：正式挂起订单的经理审批取消、取消与付款互斥、取消后迟到到账转待退款。
-- 依据：BSM:1107「已出品挂起订单取消须经理审批。能回库的商品继续执行既有退菜、退酒和高价值退品证据审批链，并同步记录退菜、库存回补和审计流水；
--   经理审批不能替代既有高价值审批链。」；BSM:1108「取消审批生效与付款成功只能有一项先完成，先完成者生效；取消生效时作废未完成付款；
--   取消后迟到到账进入待退款，不得恢复订单或继续出品。」；PGM:699「正式挂单：收银员挂起、经理审批取消」。
-- 收银员发起取消时记所列可回库的已出品明细、原因、原因码（选填，同退菜退酒原因码字典）与附件证据、发起人与时间；经理批准或驳回时记审批人、
-- 时间与意见；取消生效后迟到到账挂出的原路退款应付记 refund_ar_ap_item_id。权限码与能力开启行沿用 V600（bar.bill.hang、bar.bill.hang_cancel）。
-- 研发期无存量（UD-07）。
ALTER TABLE bar_order_suspensions
    ADD COLUMN cancel_item_ids JSONB,
    ADD COLUMN cancel_reason VARCHAR(1000),
    ADD COLUMN cancel_reason_code VARCHAR(64),
    ADD COLUMN cancel_evidence_attachment_ids JSONB,
    ADD COLUMN cancel_requested_by VARCHAR(64),
    ADD COLUMN cancel_requested_at TIMESTAMPTZ,
    ADD COLUMN cancel_decided_by VARCHAR(64),
    ADD COLUMN cancel_decided_at TIMESTAMPTZ,
    ADD COLUMN cancel_decision_reason VARCHAR(1000),
    ADD COLUMN refund_ar_ap_item_id VARCHAR(64);
