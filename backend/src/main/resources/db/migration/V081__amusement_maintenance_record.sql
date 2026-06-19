-- V081: 游乐设施定期检修记录（amuse-2 / kiosk-4，设计 §5.1.1/§6.6.1/§7.14a/§8.7.1）
-- 承载一体机/设备定期检修记录：检修单位（复用供应链供应商）、工程师、检修结果、异常说明、
-- 主管确认恢复营业。主管确认前不得由 maintenance 流转回 open（5.1.1 状态机硬约束）。

CREATE TABLE IF NOT EXISTS amusement_maintenance_record (
    id VARCHAR(64) PRIMARY KEY,
    project_id VARCHAR(64) NOT NULL REFERENCES amusement_projects(id) ON DELETE CASCADE,
    device_id VARCHAR(128),
    location_point_id VARCHAR(64),
    -- 检修单位：复用供应链供应商主数据（supplier_category ∈ {maintenance, service}，amuse-3）；
    -- 供应链侧 category 区分为引用口径，对齐方以 supply-chain 开发设计为准，本迁移仅软引用不强制外键。
    maintenance_vendor_id VARCHAR(64) NOT NULL,
    engineer_id VARCHAR(64),
    engineer_name VARCHAR(128),
    maintenance_at TIMESTAMPTZ NOT NULL,
    result VARCHAR(16) NOT NULL,
    exception_note TEXT,
    source_terminal VARCHAR(32) NOT NULL,
    status VARCHAR(32) NOT NULL,
    supervisor_confirmed_by VARCHAR(64),
    confirmed_at TIMESTAMPTZ,
    resume_business_at TIMESTAMPTZ,
    merchant_id VARCHAR(64),
    created_at TIMESTAMPTZ NOT NULL,
    CONSTRAINT chk_amusement_maint_result CHECK (result IN ('pass', 'fail')),
    CONSTRAINT chk_amusement_maint_status CHECK (status IN ('submitted', 'pending_confirm', 'confirmed', 'rejected')),
    -- 5.1.1：resume_business_at 与 confirmed_at/supervisor_confirmed_by 必须同时为空或同时非空。
    CONSTRAINT chk_amusement_maint_confirm_consistency CHECK (
        (supervisor_confirmed_by IS NULL AND confirmed_at IS NULL AND resume_business_at IS NULL)
        OR (supervisor_confirmed_by IS NOT NULL AND confirmed_at IS NOT NULL AND resume_business_at IS NOT NULL)
    )
);

CREATE INDEX IF NOT EXISTS idx_amusement_maint_project_time
    ON amusement_maintenance_record (project_id, maintenance_at DESC);

CREATE INDEX IF NOT EXISTS idx_amusement_maint_device
    ON amusement_maintenance_record (device_id, maintenance_at DESC);

CREATE INDEX IF NOT EXISTS idx_amusement_maint_vendor
    ON amusement_maintenance_record (maintenance_vendor_id, maintenance_at DESC);

-- 权限（设计 §8.7.1，dot 风格 domain.resource.action）
INSERT INTO permissions (permission_code, permission_name, domain, risk_level, requires_approval, status)
VALUES
    ('amusement.maintenance.read', 'View amusement maintenance records', 'amusement', 'medium', FALSE, 'active'),
    ('amusement.maintenance.submit', 'Submit amusement maintenance record', 'amusement', 'high', TRUE, 'active'),
    ('amusement.maintenance.confirm', 'Supervisor confirm amusement maintenance resume', 'amusement', 'high', TRUE, 'active')
ON CONFLICT (permission_code) DO UPDATE SET
    permission_name = EXCLUDED.permission_name,
    domain = EXCLUDED.domain,
    risk_level = EXCLUDED.risk_level,
    requires_approval = EXCLUDED.requires_approval,
    status = EXCLUDED.status;

INSERT INTO role_permissions (role_id, permission_code)
SELECT 'role_platform_admin', permission_code
  FROM permissions
 WHERE permission_code IN (
        'amusement.maintenance.read',
        'amusement.maintenance.submit',
        'amusement.maintenance.confirm'
 )
ON CONFLICT (role_id, permission_code) DO NOTHING;
