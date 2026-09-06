-- V389：M007 审计（2026-09-06）A-02 / F-02：PGM-1.7.1「全平台审批通用时限」。
-- 复核单记录直接受益人、到期时刻、升级次数/时刻与转发人；策略参数补审批时限（工作日，含上下限）：
-- 涉钱（finance.*、免单、退款审批、核销冲正）出厂默认 1 个工作日，其余 3 个工作日；上下限 1..30，可在后台按策略修改。
ALTER TABLE supervisor_review_decisions
    ADD COLUMN IF NOT EXISTS beneficiary_id VARCHAR(64),
    ADD COLUMN IF NOT EXISTS due_at TIMESTAMPTZ,
    ADD COLUMN IF NOT EXISTS escalation_count INT NOT NULL DEFAULT 0,
    ADD COLUMN IF NOT EXISTS escalated_at TIMESTAMPTZ,
    ADD COLUMN IF NOT EXISTS forwarded_to VARCHAR(64),
    ADD COLUMN IF NOT EXISTS forwarded_by VARCHAR(64),
    ADD COLUMN IF NOT EXISTS forwarded_at TIMESTAMPTZ;
CREATE INDEX IF NOT EXISTS idx_supervisor_review_decisions_due
    ON supervisor_review_decisions (due_at)
    WHERE review_status = 'pending_review';

UPDATE supervisor_review_policies
   SET parameters = parameters || jsonb_build_object('deadlineWorkingDays', jsonb_build_object('value', 1, 'min', 1, 'max', 30))
 WHERE NOT (parameters ? 'deadlineWorkingDays')
   AND (action_type LIKE 'finance.%' OR action_type IN ('bar.bill_waiver', 'ticketing.refund_approve', 'ticketing.verification_reverse'));
UPDATE supervisor_review_policies
   SET parameters = parameters || jsonb_build_object('deadlineWorkingDays', jsonb_build_object('value', 3, 'min', 1, 'max', 30))
 WHERE NOT (parameters ? 'deadlineWorkingDays');
