-- 文件版本：V0.01
-- V551（第十六批第 35 条，操作员裁决补充第 1 条）：内部协作工单出厂时限策略。操作员选「照「其他服务请求」种一份」：
-- 「出厂时限和验收方式照现有「其他」类型的出厂策略种一份，开箱即可用，之后可改。」
-- 「其他」类型的出厂策略只有 V166 种的一行（fwop_other_normal：normal、响应 240 分钟、处理 2880 分钟、不需验收、处理人完成即结），
-- 逐字段照抄为 internal_collaboration；已在后台配过同 id 策略的不覆盖。
INSERT INTO facility_work_order_policy (id, work_order_type, priority, response_minutes, handle_minutes, requires_acceptance, acceptance_by) VALUES
    ('fwop_internal_collaboration_normal', 'internal_collaboration', 'normal', 240, 2880, FALSE, 'handler')
ON CONFLICT (id) DO NOTHING;
