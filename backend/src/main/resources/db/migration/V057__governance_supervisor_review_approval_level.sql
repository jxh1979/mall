-- R3 修复 P0-2：审批层级逐级上提（REQ-69）。
-- 背景：R3 确认 admission_audits/approval_level 零命中；既有审批机制为 supervisor_review（V050/V053）。
-- 不新建并行 admission_audits 表，而扩展 supervisor_review_decisions：
--   approval_level         = 升级类型枚举（上一级/上两级/最高权限/指定角色），留痕（REQ-69"在 approval_level 留痕"）。
--   required_approver_rank = 由发起人 level_rank（V055 职级）按升级类型计算出的所需审批人 level_rank（逐级上提）。
-- 两列均可空：旧行与"指定角色/发起人无职级"场景为 NULL，回退既有 required_level（0-3 主管级）校验。

ALTER TABLE supervisor_review_decisions
    ADD COLUMN IF NOT EXISTS approval_level VARCHAR(32);
ALTER TABLE supervisor_review_decisions
    ADD COLUMN IF NOT EXISTS required_approver_rank INT;

DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'chk_supervisor_review_approval_level') THEN
        ALTER TABLE supervisor_review_decisions
            ADD CONSTRAINT chk_supervisor_review_approval_level
            CHECK (approval_level IS NULL OR approval_level IN
                ('one_level_up', 'two_levels_up', 'top_authority', 'specific_role'));
    END IF;
END $$;
