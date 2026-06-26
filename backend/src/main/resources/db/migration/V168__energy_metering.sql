-- V168 楼宇运营 S4 能耗抄表。
-- 独立 energy 域：表计台账、抄表读数、异常阈值；能源费结算复用 ar_ap_items 应收台账。

CREATE TABLE IF NOT EXISTS energy_meters (
    id VARCHAR(64) PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    meter_type VARCHAR(32) NOT NULL,
    parent_meter_id VARCHAR(64) REFERENCES energy_meters(id),
    gather_object VARCHAR(32) NOT NULL,
    space_id VARCHAR(64),
    merchant_id VARCHAR(64),
    project_id VARCHAR(64),
    read_mode VARCHAR(16) NOT NULL DEFAULT 'remote',
    active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT chk_energy_meters_type CHECK (meter_type IN ('electricity','water','gas','heat_cold')),
    CONSTRAINT chk_energy_meters_gather CHECK (gather_object IN ('public_area','merchant','equipment_circuit')),
    CONSTRAINT chk_energy_meters_read_mode CHECK (read_mode IN ('remote','manual'))
);
CREATE INDEX IF NOT EXISTS idx_energy_meters_parent ON energy_meters (parent_meter_id);
CREATE INDEX IF NOT EXISTS idx_energy_meters_type ON energy_meters (meter_type);
CREATE INDEX IF NOT EXISTS idx_energy_meters_gather ON energy_meters (gather_object);
CREATE INDEX IF NOT EXISTS idx_energy_meters_space ON energy_meters (space_id);
CREATE INDEX IF NOT EXISTS idx_energy_meters_merchant ON energy_meters (merchant_id);
CREATE INDEX IF NOT EXISTS idx_energy_meters_project ON energy_meters (project_id);

CREATE TABLE IF NOT EXISTS energy_meter_readings (
    id VARCHAR(64) PRIMARY KEY,
    meter_id VARCHAR(64) NOT NULL REFERENCES energy_meters(id),
    period VARCHAR(16) NOT NULL,
    reading_value NUMERIC NOT NULL,
    prev_reading_value NUMERIC NOT NULL,
    consumption NUMERIC NOT NULL,
    status VARCHAR(16) NOT NULL DEFAULT 'pending',
    abnormal_reason VARCHAR(16),
    read_mode VARCHAR(16) NOT NULL,
    reader_id VARCHAR(64),
    read_at TIMESTAMPTZ NOT NULL,
    reviewed_by VARCHAR(64),
    reviewed_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT uq_energy_meter_readings_meter_period UNIQUE (meter_id, period),
    CONSTRAINT chk_energy_meter_readings_status CHECK (status IN ('pending','recorded','reviewed','abnormal')),
    CONSTRAINT chk_energy_meter_readings_reason CHECK (
        abnormal_reason IS NULL OR abnormal_reason IN ('surge','drop','zero','reverse')
    ),
    CONSTRAINT chk_energy_meter_readings_abnormal CHECK (status <> 'abnormal' OR abnormal_reason IS NOT NULL),
    CONSTRAINT chk_energy_meter_readings_read_mode CHECK (read_mode IN ('remote','manual')),
    CONSTRAINT chk_energy_meter_readings_values CHECK (reading_value >= 0 AND prev_reading_value >= 0)
);
CREATE INDEX IF NOT EXISTS idx_energy_meter_readings_meter ON energy_meter_readings (meter_id);
CREATE INDEX IF NOT EXISTS idx_energy_meter_readings_period_status ON energy_meter_readings (period, status);
CREATE INDEX IF NOT EXISTS idx_energy_meter_readings_read_at ON energy_meter_readings (read_at DESC);
CREATE INDEX IF NOT EXISTS idx_energy_meter_readings_reviewed ON energy_meter_readings (reviewed_by, reviewed_at);

CREATE TABLE IF NOT EXISTS energy_anomaly_thresholds (
    id VARCHAR(64) PRIMARY KEY,
    meter_type VARCHAR(32) NOT NULL,
    surge_pct NUMERIC NOT NULL,
    drop_pct NUMERIC NOT NULL,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT uq_energy_anomaly_thresholds_type UNIQUE (meter_type),
    CONSTRAINT chk_energy_anomaly_thresholds_type CHECK (meter_type IN ('electricity','water','gas','heat_cold')),
    CONSTRAINT chk_energy_anomaly_thresholds_pct CHECK (surge_pct >= 0 AND drop_pct >= 0)
);
CREATE INDEX IF NOT EXISTS idx_energy_anomaly_thresholds_active ON energy_anomaly_thresholds (active);

INSERT INTO energy_anomaly_thresholds (id, meter_type, surge_pct, drop_pct, active)
VALUES
    ('eat_electricity_default', 'electricity', 50, 50, TRUE),
    ('eat_water_default',       'water',       50, 50, TRUE),
    ('eat_gas_default',         'gas',         50, 50, TRUE),
    ('eat_heat_cold_default',   'heat_cold',   50, 50, TRUE)
ON CONFLICT (meter_type) DO UPDATE SET
    surge_pct = EXCLUDED.surge_pct,
    drop_pct = EXCLUDED.drop_pct,
    active = EXCLUDED.active,
    updated_at = now();

INSERT INTO permissions (permission_code, permission_name, domain, risk_level, requires_approval, status)
VALUES
    ('energy.meter.manage', 'Manage energy meters', 'platform', 'medium', FALSE, 'active'),
    ('energy.reading.manage', 'Manage energy readings and energy fee settlement', 'platform', 'high', TRUE, 'active')
ON CONFLICT (permission_code) DO UPDATE SET
    permission_name = EXCLUDED.permission_name,
    domain = EXCLUDED.domain,
    risk_level = EXCLUDED.risk_level,
    requires_approval = EXCLUDED.requires_approval,
    status = EXCLUDED.status;

INSERT INTO role_permissions (role_id, permission_code)
SELECT 'role_platform_admin', permission_code
  FROM permissions
 WHERE permission_code IN ('energy.meter.manage', 'energy.reading.manage')
ON CONFLICT (role_id, permission_code) DO NOTHING;
