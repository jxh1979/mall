-- 文件版本：V0.01
-- 第十八批 R 段第 142 条 142-R1：收银班次财务复核平账；142-R2：交班单双方签字、财务见证归档（文末一段）。
-- 依据：BSM:1081「出现任何金额的差额一律进复核、定位责任，不设容差」「不补足不能结账交班。短款补足即为处理完毕，不另计处罚扣款。
--   长款同样必须登记，并由财务调取视频录像查明差额产生的原因；长款当期计入该商户营业外收入并单独留痕可查」；
--   ENUM:115「有差异待复核、已平账」；PGM:664「差异认定……由该酒吧对应的财务 BP 负责」。
-- 已平账＝status='closed' 且 needs_review=FALSE（状态闭集与 chk_bar_shifts_close 不改）；复核人、复核时点、长款原因三列。
-- 研发期无存量（UD-07），不回填。
ALTER TABLE bar_shifts
    ADD COLUMN resolved_by_user_id VARCHAR(64),
    ADD COLUMN resolved_at TIMESTAMPTZ,
    ADD COLUMN overage_cause VARCHAR(1000);

-- 复核平账入口 @GuardedAction(capability = finance.daily_close.manage) 的跨端全局显式开启行（照 V554 第 1 段；PGM L742 缺行即关）。
INSERT INTO terminal_capability_switches
    (id, terminal_type, capability_code, scope_type, scope_id, enabled, reason, created_at, updated_at)
VALUES
    ('tcs_optin_finance.daily_close.manage', '*', 'finance.daily_close.manage', 'global', 'global', TRUE,
     '已发布在用能力显式开启（PGM L742 opt-in）', now(), now())
ON CONFLICT (terminal_type, capability_code, scope_type, scope_id) DO NOTHING;

-- 第十八批 R 段第 142 条 142-R2：交班单双方签字、财务见证归档。
-- 依据：BSM:1081「当班收银员与接班人双方确认签字（电子确认同效；营业日最后一班没有接班人的，由财务跟班代替接班人确认签字），
--   财务跟班见证后归档」；ENUM:190「由当班收银员完成现金清点与盘收银，接班人与其对接完成交接，并由财务跟班跟进交接完成」。
-- 接班签字人不另存：有接班人即 handover_to_user_id，没有即见证财务代签（witnessed_by_user_id，successor_signed_at 与 witnessed_at 同刻）。
-- 见证即归档。研发期无存量（UD-07），不回填。
ALTER TABLE bar_shifts
    ADD COLUMN cashier_signed_at TIMESTAMPTZ,
    ADD COLUMN successor_signed_at TIMESTAMPTZ,
    ADD COLUMN witnessed_by_user_id VARCHAR(64),
    ADD COLUMN witnessed_at TIMESTAMPTZ;
