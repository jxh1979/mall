-- 条目2 外包供应商自助下属门户地基（2026-06-30 拍板）：
-- 外部协作机构主体（独立于供货 inventory_suppliers）+ 账号挂机构 + 机构管理员角色 + 自助门户权限点 + 下属上限/时效。

-- 外部协作机构：可登录派人的外部机构（维保/安保保洁/施工/外部审计等），与"供货供应商台账"分开；
-- 既供货又派人的单位可选 linked_supplier_id 指向 inventory_suppliers，机构档案与账号能力分层、不重复登记。
CREATE TABLE IF NOT EXISTS external_partner_orgs (
    id VARCHAR(64) PRIMARY KEY,
    project_id VARCHAR(64),
    org_name VARCHAR(120) NOT NULL,
    org_category VARCHAR(32) NOT NULL,          -- facility_maintenance/security_cleaning/construction/external_audit/other
    linked_supplier_id VARCHAR(64),             -- 可空：指向 inventory_suppliers（既供货又派人时）
    max_subordinates INTEGER NOT NULL DEFAULT 0,-- 平台为该机构设的下属账号数上限（0=未设/不允许开通）
    valid_until TIMESTAMPTZ,                     -- 账号有效期（随合同周期；空=长期）
    status VARCHAR(32) NOT NULL DEFAULT 'active',-- active/suspended/expired
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL
);
CREATE INDEX IF NOT EXISTS idx_external_partner_orgs_project
    ON external_partner_orgs(project_id) WHERE project_id IS NOT NULL;

-- 账号挂机构：机构管理员与下属账号都通过 partner_org_id 指向其机构。
ALTER TABLE access_users ADD COLUMN IF NOT EXISTS partner_org_id VARCHAR(64);
CREATE INDEX IF NOT EXISTS idx_access_users_partner_org
    ON access_users(partner_org_id) WHERE partner_org_id IS NOT NULL;

-- 机构管理员角色：自助管下属（开通/停用、查看完成情况），data_scope=partner_org（只见本机构），不含业务写权。
INSERT INTO roles (id, role_name, description, data_scope, status, created_at)
VALUES ('role_outsourced_vendor_admin', 'OUTSOURCED_VENDOR_ADMIN',
        'External partner org admin: self-manage subordinate accounts within platform-set cap',
        'partner_org', 'active', now())
ON CONFLICT (id) DO UPDATE SET
    role_name = EXCLUDED.role_name, description = EXCLUDED.description,
    data_scope = EXCLUDED.data_scope, status = EXCLUDED.status;

-- 自助门户权限点（仅管下属，不碰业务数据）。
INSERT INTO permissions (permission_code, permission_name, domain, risk_level, requires_approval, status)
VALUES
    ('partner.subordinate.view', 'View own external partner org subordinate accounts and work completion',
     'partner', 'low', FALSE, 'active'),
    ('partner.subordinate.manage', 'Create or disable own external partner org subordinate accounts within cap',
     'partner', 'medium', FALSE, 'active')
ON CONFLICT (permission_code) DO UPDATE SET
    permission_name = EXCLUDED.permission_name, domain = EXCLUDED.domain,
    risk_level = EXCLUDED.risk_level, status = EXCLUDED.status;

INSERT INTO role_permissions (role_id, permission_code)
VALUES
    ('role_outsourced_vendor_admin', 'partner.subordinate.view'),
    ('role_outsourced_vendor_admin', 'partner.subordinate.manage')
ON CONFLICT (role_id, permission_code) DO NOTHING;
