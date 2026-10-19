-- 文件版本：V0.01
-- HP-C01-R5 合同审批流程模板「主线层级数上下限」。依据：BSM L1349「阶段数与审批岗位按综合体后台可配（业务参数，按综合体可配，
-- 须同时配置上下限）」；2026-10-18 操作员裁决第 1 条「主线层级数配上下限」：阶段数＝主线自动补全后的层级数，三类模板各配最少/最多层级
-- （按综合体），发起时校验：补全后层级数不在上下限内即拒绝。两列皆空＝未配置不校验；配置时两者同时填写且 1 ≤ 最少 ≤ 最多。
ALTER TABLE contract_approval_flows
    ADD COLUMN IF NOT EXISTS min_main_levels INTEGER,
    ADD COLUMN IF NOT EXISTS max_main_levels INTEGER;
ALTER TABLE contract_approval_flows
    ADD CONSTRAINT chk_contract_approval_flows_main_levels CHECK (
        (min_main_levels IS NULL AND max_main_levels IS NULL)
        OR (min_main_levels >= 1 AND max_main_levels >= min_main_levels));
