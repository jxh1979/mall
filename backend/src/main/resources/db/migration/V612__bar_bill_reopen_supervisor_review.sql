-- 文件版本：V0.01
-- V612（第十八批 AA 段第 202 条 202-R1）：酒吧账单反结真库可落、改为主管审批后才生效。
-- 依据：BSM:1069「反结（重开已结账单）、沽清（卖完、临时下架）均需主管审批留痕」；
--   ENUM:111「高危动作（需审批/双证据）」行「反结、沽清、免单、账单调整、存酒作废（照片+视频）。」
-- ① bar_bill_operations 类型约束放开 reopen（V033 只收 split、merge，反结写 reopen 必然违反约束而 500）；金额约束不动。
-- ② 反结准入策略恒阻断，档位同同句高危动作免单（V068 bar_bill_waiver_block）；同组 V099 越界策略停用——
--    findEnabledPolicy 每组动作与对象只取 policy_code 排序第一条，恒阻断已覆盖越界情形。
-- ①② 必须同一迁移：约束一放开，反结就能不经审批直接生效。研发期无存量（UD-07）。
ALTER TABLE bar_bill_operations DROP CONSTRAINT chk_bar_bill_operations_type;
ALTER TABLE bar_bill_operations ADD CONSTRAINT chk_bar_bill_operations_type
    CHECK (operation_type IN ('split', 'merge', 'reopen'));

INSERT INTO supervisor_review_policies
    (policy_code, action_type, object_type, trigger_mode, outcome, approval_level, required_level, enabled)
VALUES ('bar_bill_reopen_block', 'bar.bill_reopen', 'bar_bill', 'always', 'block', 'one_level_up', 2, true)
ON CONFLICT (policy_code) DO NOTHING;

UPDATE supervisor_review_policies SET enabled = false, updated_at = now() WHERE policy_code = 'bar_bill_reopen_over_space';
