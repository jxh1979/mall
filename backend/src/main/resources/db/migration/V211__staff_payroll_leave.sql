-- BYOD 三期 F 批(2026-07-02 拍板):我的薪资(固定档案+提成自动归集+奖惩手录带事由;
-- 月结后员工才可见,月结可重复执行=重算覆盖)、请假(主管审四类假)、考勤月视图。
CREATE TABLE IF NOT EXISTS staff_base_salaries (
    user_id VARCHAR(64) PRIMARY KEY,
    monthly_salary_cent BIGINT NOT NULL,
    updated_by VARCHAR(64),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT chk_base_salary CHECK (monthly_salary_cent >= 0)
);

CREATE TABLE IF NOT EXISTS staff_payroll_adjustments (
    id VARCHAR(64) PRIMARY KEY,
    user_id VARCHAR(64) NOT NULL,
    month VARCHAR(7) NOT NULL,
    adj_type VARCHAR(16) NOT NULL,
    amount_cent BIGINT NOT NULL,
    reason VARCHAR(255) NOT NULL,
    created_by VARCHAR(64),
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT chk_adj_type CHECK (adj_type IN ('bonus', 'penalty')),
    CONSTRAINT chk_adj_amount CHECK (amount_cent > 0)
);
CREATE INDEX IF NOT EXISTS idx_payroll_adj_user_month ON staff_payroll_adjustments (user_id, month);

CREATE TABLE IF NOT EXISTS staff_commission_ledger (
    id VARCHAR(64) PRIMARY KEY,
    user_id VARCHAR(64) NOT NULL,
    month VARCHAR(7) NOT NULL,
    source_type VARCHAR(32) NOT NULL,
    source_id VARCHAR(64),
    amount_cent BIGINT NOT NULL,
    note VARCHAR(255),
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT chk_ledger_amount CHECK (amount_cent >= 0)
);
CREATE INDEX IF NOT EXISTS idx_commission_ledger_user_month ON staff_commission_ledger (user_id, month);

CREATE TABLE IF NOT EXISTS staff_payroll_statements (
    id VARCHAR(64) PRIMARY KEY,
    user_id VARCHAR(64) NOT NULL,
    month VARCHAR(7) NOT NULL,
    base_cent BIGINT NOT NULL DEFAULT 0,
    commission_cent BIGINT NOT NULL DEFAULT 0,
    bonus_cent BIGINT NOT NULL DEFAULT 0,
    penalty_cent BIGINT NOT NULL DEFAULT 0,
    total_cent BIGINT NOT NULL DEFAULT 0,
    closed_by VARCHAR(64),
    closed_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT uq_payroll_statement UNIQUE (user_id, month)
);

CREATE TABLE IF NOT EXISTS staff_leave_requests (
    id VARCHAR(64) PRIMARY KEY,
    user_id VARCHAR(64) NOT NULL,
    leave_type VARCHAR(16) NOT NULL,
    start_date DATE NOT NULL,
    end_date DATE NOT NULL,
    reason VARCHAR(255) NOT NULL,
    status VARCHAR(16) NOT NULL DEFAULT 'pending',
    reviewed_by VARCHAR(64),
    review_note VARCHAR(255),
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT chk_leave_type CHECK (leave_type IN ('personal', 'sick', 'comp', 'annual')),
    CONSTRAINT chk_leave_status CHECK (status IN ('pending', 'approved', 'rejected')),
    CONSTRAINT chk_leave_range CHECK (end_date >= start_date)
);
CREATE INDEX IF NOT EXISTS idx_leave_user ON staff_leave_requests (user_id, start_date DESC);

INSERT INTO permissions (permission_code, permission_name, domain, risk_level, requires_approval, status)
VALUES
    ('staff.payroll.manage',    'Manage payroll (base/adjustments/commissions/close month)', 'staff', 'high', FALSE, 'active'),
    ('staff.payroll.view_mine', 'View own closed payroll statements',                        'staff', 'low',  FALSE, 'active'),
    ('staff.leave.submit',      'Submit own leave requests',                                 'staff', 'low',  FALSE, 'active')
ON CONFLICT (permission_code) DO NOTHING;

INSERT INTO role_permissions (role_id, permission_code)
SELECT 'role_platform_admin', p.code FROM (VALUES
    ('staff.payroll.manage'), ('staff.payroll.view_mine'), ('staff.leave.submit')) AS p(code)
ON CONFLICT (role_id, permission_code) DO NOTHING;
