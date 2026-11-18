-- 文件版本：V0.01
-- V665（第十八批 AW 段第 299 条 299-R1）：票种售卖设置——开售时间、停售时间与暂停售卖。
-- 依据：ENUM:30「票务策略状态为：草稿（规则未发布）、待启用（规则已配置但未到生效时间）、售卖中（当前可售卖）、暂停售卖（临时停止售卖）、
--   已结束（售卖周期结束）、归档（历史策略）。」；R14 裁决 20（rulings-b02.md:11）「不设独立状态机，复用票种 4 态（草稿/上架/暂停/归档）；
--   「待启用」「已结束」按生效时间派生、不存储」。
-- 六态由 status、archived_at、delete_status 与这三列派生（TicketTypeRecord#saleStatus），不存储；暂停单独成列、不改 status，
-- 以免暂停中的跨域组合通票绕过按 published 判定的草稿版审批（第十八批裁决 61）。三列只由售卖设置动作写（JdbcTicketTypeSaleRepository）。
ALTER TABLE ticket_types ADD COLUMN sale_starts_at TIMESTAMPTZ;
ALTER TABLE ticket_types ADD COLUMN sale_ends_at TIMESTAMPTZ;
ALTER TABLE ticket_types ADD COLUMN sale_paused_at TIMESTAMPTZ;
COMMENT ON COLUMN ticket_types.sale_starts_at IS '开售时间；空＝不限，未到此时刻为待启用';
COMMENT ON COLUMN ticket_types.sale_ends_at IS '停售时间；空＝不限，到此时刻起为已结束';
COMMENT ON COLUMN ticket_types.sale_paused_at IS '暂停售卖的时刻；空＝未暂停，非空为暂停售卖';
