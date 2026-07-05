-- V230 结算重构批4(收缩后小尾巴,PRD §6):票种「资格/定价档」分类标签。
--   口径(2026-07-04用户拍板):新人票/学生票=完全同酒吧通票,无需资格认证,纯定价分级——
--     各自独立 ticket_type + 定价,现有机制已够;本字段仅供后台分类/报表,不碰定价/消耗/作废逻辑。
--   注意:结构型票种(单人/多人/多次)由现有 num_people(发放 fan-out) + total_times 派生,不在本字段;
--         本字段仅「资格/定价档」(标准/新人/学生),规则/作废完全同通票。
--   仅加列+回填,不改现有逻辑,可独立回滚。字段允许 NULL(向后兼容,NULL/standard=标准票)。
ALTER TABLE ticket_types
    ADD COLUMN IF NOT EXISTS ticket_category VARCHAR(16);

-- 回填现有票种为 NULL(既有皆标准,向后兼容;判定处对 NULL 一律按 standard 处理)。
-- 无需 UPDATE:新列默认即 NULL。

-- 取值约束(允许 NULL 向后兼容):NULL/standard=标准,new_person=新人,student=学生。
ALTER TABLE ticket_types
    ADD CONSTRAINT chk_tt_category CHECK (ticket_category IS NULL OR ticket_category IN ('standard','new_person','student'));
