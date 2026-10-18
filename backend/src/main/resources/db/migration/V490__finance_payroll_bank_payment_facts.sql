-- 文件版本：V0.01
-- 工资银行事实与成功入账分开；普通财务权限不自动授予工资权限。
CREATE TABLE finance_payroll_payment_attempts (
    id varchar(64) PRIMARY KEY,
    statement_id varchar(64) NOT NULL REFERENCES staff_payroll_statements(id),
    ar_ap_item_id varchar(64) NOT NULL REFERENCES ar_ap_items(id),
    payer_company_id varchar(64) NOT NULL,
    amount_cent bigint NOT NULL CHECK (amount_cent > 0),
    status varchar(24) NOT NULL CHECK (status IN ('prepared','processing','unknown','failed','cancelled','succeeded')),
    bank_request_ref varchar(256),
    requested_at timestamptz,
    recipient_user_id varchar(64),
    bank_flow_ref varchar(256),
    finance_payment_id varchar(64) REFERENCES ar_ap_payments(id),
    result_at timestamptz,
    created_by varchar(64) NOT NULL,
    created_at timestamptz NOT NULL DEFAULT now(),
    updated_at timestamptz NOT NULL DEFAULT now(),
    CHECK (status IN ('prepared','cancelled') OR (bank_request_ref IS NOT NULL AND requested_at IS NOT NULL AND recipient_user_id IS NOT NULL)),
    CHECK (status <> 'succeeded' OR (bank_flow_ref IS NOT NULL AND finance_payment_id IS NOT NULL AND result_at IS NOT NULL))
);
CREATE UNIQUE INDEX uq_payroll_payment_active ON finance_payroll_payment_attempts(statement_id) WHERE status IN ('prepared','processing','unknown');
CREATE UNIQUE INDEX uq_payroll_bank_request ON finance_payroll_payment_attempts(payer_company_id,bank_request_ref) WHERE bank_request_ref IS NOT NULL;
CREATE UNIQUE INDEX uq_payroll_bank_success ON finance_payroll_payment_attempts(payer_company_id,bank_flow_ref) WHERE status='succeeded';
CREATE TABLE finance_payroll_payment_events (
    id varchar(64) PRIMARY KEY,
    attempt_id varchar(64) NOT NULL REFERENCES finance_payroll_payment_attempts(id),
    status varchar(24) NOT NULL CHECK (status IN ('processing','unknown','failed','cancelled','succeeded')),
    result_at timestamptz NOT NULL,
    evidence_attachment_ids jsonb NOT NULL,
    reason text NOT NULL,
    recorded_by varchar(64) NOT NULL,
    recorded_at timestamptz NOT NULL DEFAULT now()
);
INSERT INTO permissions(permission_code,permission_name,domain,risk_level,requires_approval,status)
VALUES ('finance.payroll.view','查看工资应付与银行付款','finance','high',true,'active'),
       ('finance.payroll.payment.register','核实工资银行付款','finance','high',true,'active'),
       ('finance.payroll.export','导出工资应付','finance','high',true,'active')
ON CONFLICT(permission_code) DO NOTHING;


