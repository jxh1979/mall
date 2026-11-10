-- 文件版本：V0.01
-- V617（第十八批 AD 段第 204 条 204-R1）：全平台审批通用时限按审批类型（被审批的动作类型 action_type）可配。
-- 依据：PGM:529「任一业务审批节点超过后台配置的时限（业务参数，按审批类型可配，出厂默认 3 个工作日，涉钱审批出厂默认 1 个工作日，
--   须同时配置上下限）未处理的，系统自动提醒并升级到该节点的上一级」；PGM:536「按合同类型可配，出厂默认 3 个工作日，须同时配置上下限」。
-- 只存后台改过的审批类型；没有行的按出厂（涉钱 1、其余 3 个工作日，上下限 1～30，见 SupervisorReviewService）。表结构与 CHECK 同 V607
-- finance_reconciliation_stage_limits。时限只剩这一处来源：同一迁移移走 V389、V494 写进策略表 parameters 的 deadlineWorkingDays 键
-- （研发期无存量，UD-07，不保留旧键回落）。

CREATE TABLE approval_deadline_settings (
    action_type VARCHAR(100) PRIMARY KEY,
    deadline_working_days INTEGER NOT NULL,
    min_working_days INTEGER NOT NULL,
    max_working_days INTEGER NOT NULL,
    updated_by VARCHAR(128),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT chk_ads_working_days CHECK (
        1 <= min_working_days AND min_working_days <= deadline_working_days AND deadline_working_days <= max_working_days)
);

DO $$
BEGIN
    IF EXISTS (SELECT 1 FROM pg_roles WHERE rolname = 'complex_app') THEN
        EXECUTE 'GRANT SELECT, INSERT, UPDATE ON approval_deadline_settings TO complex_app';
    END IF;
END $$;

UPDATE supervisor_review_policies
   SET parameters = parameters - 'deadlineWorkingDays'
 WHERE parameters ? 'deadlineWorkingDays';
