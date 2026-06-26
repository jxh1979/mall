-- V165 楼宇机电设备台账（③楼宇运营 S1，据 M1§3 + 业务速查表§十重建）。
-- 对齐：设备状态6态(正常/告警/故障/维保中/停用/报废)、4分类(电梯扶梯/暖通空调/配电供电/给排水消防)、
-- 字段含位置/品牌/责任人/状态、每台单独维保周期+到期提醒、告警源(人工/IoT)。
-- 区别于游乐设备(amusement_devices=运营受控对象)：这是楼宇侧机电设施。旧V165仅3态，本次修至6态。

CREATE TABLE IF NOT EXISTS facility_equipment (
    id VARCHAR(64) PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    equipment_type VARCHAR(32) NOT NULL,            -- elevator|hvac|power|water_fire（M1 4分类）
    space_id VARCHAR(64),                           -- 安装位置（V005 spaces）
    brand VARCHAR(255),                             -- 品牌
    model VARCHAR(255),
    vendor VARCHAR(255),                            -- 供应/厂商
    responsible_user_id VARCHAR(64),                -- 责任人
    status VARCHAR(16) NOT NULL DEFAULT 'normal',   -- 6态：normal|alarm|fault|maintenance|disabled|retired
    maintenance_cycle_days INTEGER,                 -- 每台单独维保周期（天）
    last_maintenance_at TIMESTAMPTZ,
    next_maintenance_due DATE,                       -- 到期提醒（last + cycle 派生固化）
    install_date DATE,
    warranty_until DATE,
    alarm_source VARCHAR(16),                        -- manual|iot（status=alarm 时记）
    alarm_note TEXT,
    alarm_raised_at TIMESTAMPTZ,
    project_id VARCHAR(64),
    merchant_id VARCHAR(64),
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT chk_facility_equip_type CHECK (equipment_type IN ('elevator','hvac','power','water_fire')),
    CONSTRAINT chk_facility_equip_status CHECK (status IN ('normal','alarm','fault','maintenance','disabled','retired')),
    CONSTRAINT chk_facility_equip_alarm_src CHECK (alarm_source IS NULL OR alarm_source IN ('manual','iot')),
    CONSTRAINT chk_facility_equip_cycle CHECK (maintenance_cycle_days IS NULL OR maintenance_cycle_days > 0)
);
CREATE INDEX IF NOT EXISTS idx_facility_equipment_space ON facility_equipment (space_id);
CREATE INDEX IF NOT EXISTS idx_facility_equipment_status ON facility_equipment (status);
CREATE INDEX IF NOT EXISTS idx_facility_equipment_due ON facility_equipment (next_maintenance_due);

INSERT INTO permissions (permission_code, permission_name, domain, risk_level, requires_approval, status)
VALUES ('facility.equipment.manage', 'Manage building facility equipment registry', 'platform', 'medium', FALSE, 'active')
ON CONFLICT (permission_code) DO UPDATE SET
    permission_name = EXCLUDED.permission_name, domain = EXCLUDED.domain,
    risk_level = EXCLUDED.risk_level, requires_approval = EXCLUDED.requires_approval, status = EXCLUDED.status;
INSERT INTO role_permissions (role_id, permission_code)
SELECT 'role_platform_admin', 'facility.equipment.manage'
ON CONFLICT (role_id, permission_code) DO NOTHING;
