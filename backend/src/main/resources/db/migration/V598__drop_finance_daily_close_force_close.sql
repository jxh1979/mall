-- 文件版本：V0.01
-- 第十八批 S 段第 163 条 163-R1：日结不再接收、不再落库 force_close。
-- 依据：三份 HTML 与裁决都没有「强制日结」；日结只有按闸确认、锁定一条路（ENUM:190「所属数据未标记为最终之前，日结不得确认、不得锁定。」）。
-- 照第十八批 Q 段第 141 条 141-R1 先例：日结请求、日结记录与读写代码已同批删除该字段；V008 历史迁移不改；研发期无存量（UD-07），直接删列。
ALTER TABLE finance_daily_closes
    DROP COLUMN IF EXISTS force_close;
