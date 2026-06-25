-- V165 楼宇机电设备台账（③ 楼宇运营 S1，PRD phase8）：综合体楼宇机电设施设备主数据——工单/巡检/能耗的引用地基。
-- 区别于游乐设备（amusement_devices=运营受控对象）：这是楼宇侧设施（HVAC/电梯/消防/给排水/配电/弱电等）。
-- 仿 V143 amusement_devices 建表约定 + V144/V161 权限种子；project_id/merchant_id 预留综合体维度（Phase④ 收口）。

CREATE TABLE IF NOT EXISTS facility_equipment (
    id VARCHAR(64) PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    equipment_type VARCHAR(32) NOT NULL,           -- hvac|elevator|fire|power|water|weak_current|other
    space_id VARCHAR(64),                          -- 安装位置（V005 spaces）
    vendor VARCHAR(255),
    model VARCHAR(255),
    install_date DATE,
    warranty_until DATE,
    status VARCHAR(32) NOT NULL DEFAULT 'active',   -- active|maintenance|retired
    maintenance_cycle_days INTEGER,                -- 维保周期（天，空=未定）；S3 巡检按此推到期
    last_maintenance_at TIMESTAMPTZ,
    project_id VARCHAR(64),
    merchant_id VARCHAR(64),
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT chk_facility_equipment_type CHECK (equipment_type IN ('hvac', 'elevator', 'fire', 'power', 'water', 'weak_current', 'other')),
    CONSTRAINT chk_facility_equipment_status CHECK (status IN ('active', 'maintenance', 'retired')),
    CONSTRAINT chk_facility_equipment_cycle CHECK (maintenance_cycle_days IS NULL OR maintenance_cycle_days > 0)
);
CREATE INDEX IF NOT EXISTS idx_facility_equipment_space ON facility_equipment (space_id);

INSERT INTO permissions (permission_code, permission_name, domain, risk_level, requires_approval, status)
VALUES
    ('facility.equipment.manage', 'Manage building facility equipment registry', 'platform', 'medium', FALSE, 'active')
ON CONFLICT (permission_code) DO UPDATE SET
    permission_name = EXCLUDED.permission_name, domain = EXCLUDED.domain,
    risk_level = EXCLUDED.risk_level, requires_approval = EXCLUDED.requires_approval, status = EXCLUDED.status;

INSERT INTO role_permissions (role_id, permission_code)
SELECT 'role_platform_admin', permission_code FROM permissions
 WHERE permission_code IN ('facility.equipment.manage')
ON CONFLICT (role_id, permission_code) DO NOTHING;
