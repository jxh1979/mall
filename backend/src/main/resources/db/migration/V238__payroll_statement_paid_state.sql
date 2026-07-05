-- 薪资资金安全①【P0】已发放工资单不得被重结静默改写(fix/payroll-safety)。
--   缺陷:closePayrollMonth 的 ON CONFLICT DO UPDATE 无条件覆盖已定格单,全域无 paid/locked 状态;
--         关账→发放后事后补罚再 close,total 被静默改写无痕(套现)。
--   修:薪资单加状态机 status(closed/paid);发放动作独立落 staff_payroll_payouts;
--       重结时 status='paid' 的行跳过覆盖(见 closePayrollMonth 的 WHERE 追加谓词)。
ALTER TABLE staff_payroll_statements
    ADD COLUMN IF NOT EXISTS status VARCHAR(16) NOT NULL DEFAULT 'closed';
ALTER TABLE staff_payroll_statements DROP CONSTRAINT IF EXISTS chk_payroll_statement_status;
ALTER TABLE staff_payroll_statements
    ADD CONSTRAINT chk_payroll_statement_status CHECK (status IN ('closed', 'paid'));

-- 发放台账:一行=某员工某月薪资单的一次发放;唯一 (user_id, month) 防重复发放。
CREATE TABLE IF NOT EXISTS staff_payroll_payouts (
    id VARCHAR(64) PRIMARY KEY,
    statement_id VARCHAR(64) NOT NULL,
    user_id VARCHAR(64) NOT NULL,
    month VARCHAR(7) NOT NULL,
    paid_total_cent BIGINT NOT NULL,
    pay_method VARCHAR(24),
    pay_ref VARCHAR(64),
    paid_by VARCHAR(64),
    paid_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    project_id VARCHAR(64),
    CONSTRAINT uq_payroll_payout UNIQUE (user_id, month),
    CONSTRAINT chk_payout_total CHECK (paid_total_cent >= 0)
);
CREATE INDEX IF NOT EXISTS idx_payroll_payout_month ON staff_payroll_payouts (month);
