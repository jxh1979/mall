-- 文件版本：V0.01
-- 第十八批 Q 段第 141 条 141-R1：收银开班不再接收现金差额容差，班结差额不为 0 即进复核。
-- 依据：BSM:1081「收银不应出现差额，出现任何金额的差额一律进复核、定位责任，不设容差」；
-- DEC-R4-2026-08-04-26「收银交班取消容差：出现任何金额差额一律进复核，谁差谁补」。
-- 开班请求、班次记录与读写代码已同批删除该字段；V095 历史迁移不改；研发期无存量（UD-07），直接删列。
ALTER TABLE bar_shifts
    DROP CONSTRAINT IF EXISTS chk_bar_shifts_threshold,
    DROP COLUMN IF EXISTS cash_diff_threshold_cent;
