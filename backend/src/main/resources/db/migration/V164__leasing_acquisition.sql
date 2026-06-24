-- V164 招商线索→租赁报价（② 履约闭环 S4，PRD phase6）：履约链最前端绿地新建。
-- leasing_leads：招商线索（潜在商户/意向）；leasing_offers：租赁报价（lead+空间+条款）。报价 accepted→转合同 draft（喂入既有合同环）。
-- 均含 project_id 预留综合体维度 + 状态 CHECK + 审计列；权限种子 leasing.lead.manage / leasing.offer.manage（仿 V158）。

CREATE TABLE IF NOT EXISTS leasing_leads (
    id VARCHAR(64) PRIMARY KEY,
    contact_name VARCHAR(120) NOT NULL,
    intended_business_type VARCHAR(64),
    intended_space_id VARCHAR(64),
    source VARCHAR(64),
    status VARCHAR(32) NOT NULL DEFAULT 'new',
    project_id VARCHAR(64),
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT chk_leasing_leads_status CHECK (status IN ('new', 'contacted', 'qualified', 'converted', 'lost'))
);

CREATE TABLE IF NOT EXISTS leasing_offers (
    id VARCHAR(64) PRIMARY KEY,
    lead_id VARCHAR(64) NOT NULL REFERENCES leasing_leads(id),
    space_id VARCHAR(64),
    merchant_id VARCHAR(64),
    effective_date DATE,
    expiry_date DATE,
    rent_terms JSONB NOT NULL DEFAULT '{}'::jsonb,
    valid_until DATE,
    status VARCHAR(32) NOT NULL DEFAULT 'draft',
    converted_contract_id VARCHAR(64),
    project_id VARCHAR(64),
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT chk_leasing_offers_status CHECK (status IN ('draft', 'sent', 'accepted', 'rejected', 'expired', 'converted'))
);
CREATE INDEX IF NOT EXISTS idx_leasing_offers_lead ON leasing_offers (lead_id);

INSERT INTO permissions (permission_code, permission_name, domain, risk_level, requires_approval, status)
VALUES
    ('leasing.lead.manage', 'Manage leasing acquisition leads', 'platform', 'medium', FALSE, 'active'),
    ('leasing.offer.manage', 'Manage leasing offers and convert to contract', 'platform', 'high', TRUE, 'active')
ON CONFLICT (permission_code) DO UPDATE SET
    permission_name = EXCLUDED.permission_name, domain = EXCLUDED.domain,
    risk_level = EXCLUDED.risk_level, requires_approval = EXCLUDED.requires_approval, status = EXCLUDED.status;

INSERT INTO role_permissions (role_id, permission_code)
SELECT 'role_platform_admin', permission_code FROM permissions
 WHERE permission_code IN ('leasing.lead.manage', 'leasing.offer.manage')
ON CONFLICT (role_id, permission_code) DO NOTHING;
