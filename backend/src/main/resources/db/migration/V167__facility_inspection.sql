-- V167 楼宇设施巡检（③ 楼宇运营 S3，依赖 S1 设备）：巡检模板（清单 JSONB）+ 巡检任务（按设备维保周期到期生成，防重）+ 巡检记录。
-- Job 按 facility_equipment.maintenance_cycle_days 从 last_maintenance_at 推 due_at upsert（仿 V143 amusement 巡检任务）；
-- 超期(overdue) 由 Job 把 pending 且 due_at<now 翻 overdue（同 amusement markOverdue）。permission facility.inspection.manage。

CREATE TABLE IF NOT EXISTS facility_inspection_templates (
    id VARCHAR(64) PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    equipment_type VARCHAR(32),                   -- 适用设备类型（空=通用）
    items JSONB NOT NULL DEFAULT '[]'::jsonb,     -- 巡检项清单（字符串数组）
    status VARCHAR(16) NOT NULL DEFAULT 'active', -- active|inactive
    project_id VARCHAR(64),
    merchant_id VARCHAR(64),
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT chk_facility_insp_tmpl_status CHECK (status IN ('active', 'inactive')),
    CONSTRAINT chk_facility_insp_tmpl_type CHECK (equipment_type IS NULL OR equipment_type IN ('hvac', 'elevator', 'fire', 'power', 'water', 'weak_current', 'other'))
);

CREATE TABLE IF NOT EXISTS facility_inspection_tasks (
    id VARCHAR(64) PRIMARY KEY,
    equipment_id VARCHAR(64) NOT NULL REFERENCES facility_equipment(id) ON DELETE CASCADE,
    space_id VARCHAR(64),
    due_at TIMESTAMPTZ NOT NULL,
    status VARCHAR(16) NOT NULL DEFAULT 'pending', -- pending|completed|overdue|cancelled
    generated_at TIMESTAMPTZ NOT NULL,
    completed_record_id VARCHAR(64),
    completed_at TIMESTAMPTZ,
    project_id VARCHAR(64),
    merchant_id VARCHAR(64),
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT chk_facility_insp_task_status CHECK (status IN ('pending', 'completed', 'overdue', 'cancelled'))
);
-- 防重：同设备同一到期点只生成一张待办（Job 幂等地基）。
CREATE UNIQUE INDEX IF NOT EXISTS uk_facility_insp_task_due ON facility_inspection_tasks (equipment_id, due_at);
CREATE INDEX IF NOT EXISTS idx_facility_insp_task_status ON facility_inspection_tasks (status, due_at);

CREATE TABLE IF NOT EXISTS facility_inspection_records (
    id VARCHAR(64) PRIMARY KEY,
    task_id VARCHAR(64) REFERENCES facility_inspection_tasks(id) ON DELETE SET NULL,
    equipment_id VARCHAR(64) NOT NULL REFERENCES facility_equipment(id) ON DELETE CASCADE,
    inspector VARCHAR(64),
    result VARCHAR(16) NOT NULL DEFAULT 'pass',    -- pass|fail
    findings VARCHAR(2000),
    checked_items JSONB NOT NULL DEFAULT '[]'::jsonb,
    project_id VARCHAR(64),
    merchant_id VARCHAR(64),
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT chk_facility_insp_record_result CHECK (result IN ('pass', 'fail'))
);
CREATE INDEX IF NOT EXISTS idx_facility_insp_record_equipment ON facility_inspection_records (equipment_id, created_at DESC);

INSERT INTO permissions (permission_code, permission_name, domain, risk_level, requires_approval, status)
VALUES
    ('facility.inspection.manage', 'Manage building facility inspections', 'platform', 'medium', FALSE, 'active')
ON CONFLICT (permission_code) DO UPDATE SET
    permission_name = EXCLUDED.permission_name, domain = EXCLUDED.domain,
    risk_level = EXCLUDED.risk_level, requires_approval = EXCLUDED.requires_approval, status = EXCLUDED.status;

INSERT INTO role_permissions (role_id, permission_code)
SELECT 'role_platform_admin', permission_code FROM permissions
 WHERE permission_code IN ('facility.inspection.manage')
ON CONFLICT (role_id, permission_code) DO NOTHING;
