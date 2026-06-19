-- R5 审计点②修正：权益模板按各自商户/场地/点位归属，票种自带有效期天数。
-- 背景：TicketOrderService.issueEntitlements 此前把联合通票所有权益的 merchant_id 硬设为出票方、
-- venue/point=null、valid_to 恒为 now+1 天，违背“各商户只看自己结算”与票种实际有效期。
-- 这些列均可空：
--   * entitlement_templates.merchant_id/venue_id/point_id 为空时，发放逻辑回退到出票方商户（向后兼容既有单模板票）。
--   * ticket_types.validity_days 为空时，发放逻辑回退默认 1 天有效期（向后兼容）。

ALTER TABLE entitlement_templates
    ADD COLUMN IF NOT EXISTS merchant_id VARCHAR(64),
    ADD COLUMN IF NOT EXISTS venue_id VARCHAR(64),
    ADD COLUMN IF NOT EXISTS point_id VARCHAR(64);

ALTER TABLE ticket_types
    ADD COLUMN IF NOT EXISTS validity_days INTEGER;
