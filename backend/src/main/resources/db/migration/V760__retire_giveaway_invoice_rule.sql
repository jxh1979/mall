-- 文件版本：V0.01
-- V760：第十八批 CD 段第 402 条 402-R1（通票免费酒水结算进开票清单）：停用开票映射规则 isr_giveaway（通票赠送权益兑付「不开票（内部成本转付）」）。
-- 依据：BSM:1803「平台内部各主体之间的往来需要开具发票，开票的具体规则一律按国家财税相关规定执行，本文件不复述细则。」；决策台账 DEC-R4-2026-08-04-51；
--   待开发清单第 402 条。V362 种子该规则的出处是财务底稿 23 第 12 行，三份 HTML 与决策台账都没有「不开票」的口径；自本条起通票免费酒水
--   （台账来源 pass_drink_settlement）按台账来源月度组由出品酒吧开给娱乐平台公司，收入类型 spv_service「运营服务费」（规则 isr_spvsvc）。
-- 不改 V362（Flyway 只加不改）；listRules 只取 active，停用后开票清单汇总的映射规则表不再列它。研发阶段无存量（UD-07）。
UPDATE invoice_subject_rules SET status = 'inactive', updated_by = 'system', updated_at = now() WHERE id = 'isr_giveaway';
