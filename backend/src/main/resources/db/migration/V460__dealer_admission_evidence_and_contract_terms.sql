-- 文件版本：V0.01
-- 经销准入绑定四类提交证据；预建门户账号默认禁用。合同到期规则随订单锁定，不回填存量。
CREATE TABLE dealer_admission_submissions (
    dealer_id VARCHAR(64) PRIMARY KEY REFERENCES dealers(id),
    company_attachment_ids JSONB NOT NULL,
    contract_attachment_ids JSONB NOT NULL,
    invoice_attachment_ids JSONB NOT NULL,
    bank_attachment_ids JSONB NOT NULL,
    submitted_by VARCHAR(64) NOT NULL REFERENCES access_users(id),
    submitted_at TIMESTAMPTZ NOT NULL,
    CONSTRAINT chk_dealer_admission_evidence_arrays CHECK (
        jsonb_typeof(company_attachment_ids) = 'array' AND jsonb_array_length(company_attachment_ids) > 0
        AND jsonb_typeof(contract_attachment_ids) = 'array' AND jsonb_array_length(contract_attachment_ids) > 0
        AND jsonb_typeof(invoice_attachment_ids) = 'array' AND jsonb_array_length(invoice_attachment_ids) > 0
        AND jsonb_typeof(bank_attachment_ids) = 'array' AND jsonb_array_length(bank_attachment_ids) > 0
    )
);

ALTER TABLE dealer_portal_accounts DROP CONSTRAINT chk_dealer_portal_accounts_status;
ALTER TABLE dealer_portal_accounts
    ALTER COLUMN status SET DEFAULT 'disabled',
    ADD CONSTRAINT chk_dealer_portal_accounts_status CHECK (status IN ('disabled', 'active', 'revoked'));

ALTER TABLE dealer_contracts
    ADD COLUMN terms_version INTEGER NOT NULL DEFAULT 0 CHECK (terms_version >= 0),
    ADD COLUMN contract_signed_at TIMESTAMPTZ,
    ADD COLUMN first_payment_anchor VARCHAR(32),
    ADD COLUMN first_payment_fixed_at TIMESTAMPTZ,
    ADD COLUMN balance_due_anchor VARCHAR(32),
    ADD COLUMN balance_due_fixed_at TIMESTAMPTZ,
    ADD CONSTRAINT chk_dealer_contract_first_payment_anchor
        CHECK (first_payment_anchor IN ('contract_signed', 'order_created', 'payment_notified', 'fixed_date')),
    ADD CONSTRAINT chk_dealer_contract_balance_due_anchor
        CHECK (balance_due_anchor IN ('contract_signed', 'order_created', 'payment_notified', 'fixed_date', 'shipped', 'signed'));

ALTER TABLE dealer_orders
    ADD COLUMN contract_snapshot JSONB,
    ADD CONSTRAINT chk_dealer_order_contract_snapshot CHECK (contract_snapshot IS NULL OR jsonb_typeof(contract_snapshot) = 'object');
