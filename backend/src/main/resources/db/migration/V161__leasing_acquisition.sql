-- V161 招商获客（据需求重建·feat/real-estate-rebuild-from-requirements）
-- 依据：leasing-acquisition-prd-v1.0 + 业务HTML §10（线索7态/意向5阶段/品牌库4类）+ M1需求采集§1。
-- 纠正旧V164：线索状态5态→7态；意向阶段独立流转(非报价状态机)；新增品牌库(4类+黑名单拦截)+跟进表(3天提醒)。
-- 数据隔离=综合体级，均带 project_id。

-- 1) 招商线索（7 态，对齐 ENUM §十）
CREATE TABLE IF NOT EXISTS leasing_leads (
    id VARCHAR(64) PRIMARY KEY,
    contact_name VARCHAR(120) NOT NULL,
    contact_phone VARCHAR(40),
    source VARCHAR(32) NOT NULL,
    intended_business_type VARCHAR(64),
    intended_space_id VARCHAR(64),
    intended_area_sqm NUMERIC(12,2),
    brand_id VARCHAR(64),
    owner_user_id VARCHAR(64),
    status VARCHAR(32) NOT NULL DEFAULT 'pending_follow_up',
    lost_or_hold_reason VARCHAR(500),
    last_follow_up_at TIMESTAMPTZ,
    project_id VARCHAR(64),
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT chk_leasing_leads_source CHECK (source IN
        ('outbound', 'online_inquiry', 'referral', 'exhibition')),
    CONSTRAINT chk_leasing_leads_status CHECK (status IN
        ('pending_follow_up', 'following_up', 'converted_to_intent', 'negotiating', 'signed', 'lost', 'on_hold'))
);
CREATE INDEX IF NOT EXISTS idx_leasing_leads_owner ON leasing_leads (owner_user_id, status);
CREATE INDEX IF NOT EXISTS idx_leasing_leads_brand ON leasing_leads (brand_id);

-- 2) 招商意向（5 阶段独立流转，非报价状态机）
CREATE TABLE IF NOT EXISTS leasing_intents (
    id VARCHAR(64) PRIMARY KEY,
    lead_id VARCHAR(64) NOT NULL REFERENCES leasing_leads(id),
    stage VARCHAR(32) NOT NULL DEFAULT 'initial_contact',
    intended_space_id VARCHAR(64),
    intended_area_sqm NUMERIC(12,2),
    rent_terms JSONB NOT NULL DEFAULT '{}'::jsonb,
    last_stage_change_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    last_stage_change_by VARCHAR(64),
    project_id VARCHAR(64),
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT chk_leasing_intents_stage CHECK (stage IN
        ('initial_contact', 'requirement_confirmed', 'proposal_quoted', 'negotiation', 'pending_signing'))
);
CREATE INDEX IF NOT EXISTS idx_leasing_intents_lead ON leasing_intents (lead_id);

-- 3) 品牌/客户库（4 类 + 黑名单拦截）
CREATE TABLE IF NOT EXISTS leasing_brands (
    id VARCHAR(64) PRIMARY KEY,
    brand_name VARCHAR(160) NOT NULL,
    brand_type VARCHAR(32) NOT NULL DEFAULT 'prospective',
    business_type VARCHAR(64),
    blacklist_reason VARCHAR(500),
    blacklisted_by VARCHAR(64),
    blacklisted_at TIMESTAMPTZ,
    project_id VARCHAR(64),
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT chk_leasing_brands_type CHECK (brand_type IN
        ('intentional', 'prospective', 'partner', 'blacklist'))
);
-- 同综合体内品牌名唯一（线索去重依据）
CREATE UNIQUE INDEX IF NOT EXISTS uq_leasing_brands_name_project
    ON leasing_brands (project_id, lower(brand_name));

-- 4) 跟进记录（线索/意向通用流水；3 天提醒由应用层按 last_follow_up_at 算）
CREATE TABLE IF NOT EXISTS leasing_follow_ups (
    id VARCHAR(64) PRIMARY KEY,
    ref_type VARCHAR(16) NOT NULL,
    ref_id VARCHAR(64) NOT NULL,
    method VARCHAR(32),
    content VARCHAR(1000) NOT NULL,
    next_plan_at TIMESTAMPTZ,
    operator_user_id VARCHAR(64) NOT NULL,
    project_id VARCHAR(64),
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT chk_leasing_follow_ups_ref CHECK (ref_type IN ('lead', 'intent'))
);
CREATE INDEX IF NOT EXISTS idx_leasing_follow_ups_ref ON leasing_follow_ups (ref_type, ref_id, created_at DESC);

-- 5) 权限点（最小颗粒，仿 V158 风格）
INSERT INTO permissions (permission_code, permission_name, domain, risk_level, requires_approval, status)
VALUES
    ('leasing.lead.manage',      'Manage leasing leads (create/assign/status)', 'platform', 'medium', FALSE, 'active'),
    ('leasing.intent.manage',    'Advance leasing intents',                     'platform', 'medium', FALSE, 'active'),
    ('leasing.brand.manage',     'Maintain leasing brand library',              'platform', 'medium', FALSE, 'active'),
    ('leasing.brand.blacklist',  'Blacklist/unblacklist a brand',               'platform', 'high',   TRUE,  'active'),
    ('leasing.convert',          'Sign intent and convert lead to merchant',    'platform', 'high',   TRUE,  'active')
ON CONFLICT (permission_code) DO UPDATE SET
    permission_name = EXCLUDED.permission_name, domain = EXCLUDED.domain,
    risk_level = EXCLUDED.risk_level, requires_approval = EXCLUDED.requires_approval, status = EXCLUDED.status;

INSERT INTO role_permissions (role_id, permission_code)
SELECT 'role_platform_admin', permission_code FROM permissions
 WHERE permission_code IN ('leasing.lead.manage','leasing.intent.manage','leasing.brand.manage','leasing.brand.blacklist','leasing.convert')
ON CONFLICT (role_id, permission_code) DO NOTHING;
