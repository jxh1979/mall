-- V168 能耗抄表/计量（③ 楼宇运营 S4，独立）：能耗表台账 + 抄表读数（系统算 consumption=本期-上期）。
-- 一期只抄表 + 算用量；能耗费起账（用量×单价→AR）留 S4b/二期（待业务定单价口径）。permission facility.energy.manage。

CREATE TABLE IF NOT EXISTS energy_meters (
    id VARCHAR(64) PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    meter_type VARCHAR(16) NOT NULL,               -- electricity|water|gas
    space_id VARCHAR(64),
    unit VARCHAR(16) NOT NULL DEFAULT 'kWh',        -- 计量单位（kWh/m³ 等）
    status VARCHAR(16) NOT NULL DEFAULT 'active',   -- active|inactive
    project_id VARCHAR(64),
    merchant_id VARCHAR(64),                         -- 可空：公区表无归属商户
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT chk_energy_meter_type CHECK (meter_type IN ('electricity', 'water', 'gas')),
    CONSTRAINT chk_energy_meter_status CHECK (status IN ('active', 'inactive'))
);
CREATE INDEX IF NOT EXISTS idx_energy_meters_space ON energy_meters (space_id);

CREATE TABLE IF NOT EXISTS energy_meter_readings (
    id VARCHAR(64) PRIMARY KEY,
    meter_id VARCHAR(64) NOT NULL REFERENCES energy_meters(id) ON DELETE CASCADE,
    period VARCHAR(16) NOT NULL,                     -- 抄表期次（如 2026-06）
    reading_value NUMERIC(18, 4) NOT NULL,           -- 表底读数（单调不减）
    consumption NUMERIC(18, 4),                       -- 本期用量=本期-上期；首读为空
    recorded_by VARCHAR(64),
    project_id VARCHAR(64),
    merchant_id VARCHAR(64),
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT chk_energy_reading_value_nonneg CHECK (reading_value >= 0),
    CONSTRAINT uq_energy_reading_meter_period UNIQUE (meter_id, period)  -- 同表同期次只一条（幂等地基）
);
CREATE INDEX IF NOT EXISTS idx_energy_readings_meter_period ON energy_meter_readings (meter_id, period DESC);

INSERT INTO permissions (permission_code, permission_name, domain, risk_level, requires_approval, status)
VALUES
    ('facility.energy.manage', 'Manage building energy meters and readings', 'platform', 'medium', FALSE, 'active')
ON CONFLICT (permission_code) DO UPDATE SET
    permission_name = EXCLUDED.permission_name, domain = EXCLUDED.domain,
    risk_level = EXCLUDED.risk_level, requires_approval = EXCLUDED.requires_approval, status = EXCLUDED.status;

INSERT INTO role_permissions (role_id, permission_code)
SELECT 'role_platform_admin', permission_code FROM permissions
 WHERE permission_code IN ('facility.energy.manage')
ON CONFLICT (role_id, permission_code) DO NOTHING;
