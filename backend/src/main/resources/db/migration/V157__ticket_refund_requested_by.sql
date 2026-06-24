-- 退款分级授权 Stage B（审计 A2 修复）：票务退款补「发起人」留痕。
-- 原 ticket_refunds 仅存审批人（且代码写死字面量 "staff"），无法追溯谁发起退款、
-- 也无从做职责分离（SoD：审批人≠发起人）。本迁移加 requested_by 承载发起人。
-- 纯加列、可空（存量历史退款无发起人记录，留 NULL）；不改任何既有行为。
-- 实际放行/SoD/自助自动退由功能标志 refund.tiered_authorization.enabled 控制（默认关）。

ALTER TABLE ticket_refunds ADD COLUMN IF NOT EXISTS requested_by VARCHAR(64);
