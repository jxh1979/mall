-- V167 楼宇巡检（③楼宇运营 S3，据 M1§5 + 速查表§十）。
-- 类型4：security|fire|facility|environment；能耗抄表不在巡检域，归能耗域。
-- 任务7态、排程3式(template/manual/route)，异常按模板 light/to_work_order 闭环。

CREATE TABLE IF NOT EXISTS facility_inspection_templates (
    id VARCHAR(64) PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    inspection_type VARCHAR(24) NOT NULL,
    cycle_days INTEGER,
    abnormal_mode VARCHAR(24) NOT NULL DEFAULT 'light',
    checklist JSONB NOT NULL DEFAULT '[]'::jsonb,
    scope_note TEXT,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    project_id VARCHAR(64),
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT chk_fit_type CHECK (inspection_type IN ('security','fire','facility','environment')),
    CONSTRAINT chk_fit_cycle CHECK (cycle_days IS NULL OR cycle_days > 0),
    CONSTRAINT chk_fit_abnormal_mode CHECK (abnormal_mode IN ('light','to_work_order'))
);
CREATE INDEX IF NOT EXISTS idx_fit_type ON facility_inspection_templates (inspection_type);
CREATE INDEX IF NOT EXISTS idx_fit_active_cycle ON facility_inspection_templates (active, cycle_days);
CREATE INDEX IF NOT EXISTS idx_fit_project ON facility_inspection_templates (project_id);

CREATE TABLE IF NOT EXISTS facility_inspection_tasks (
    id VARCHAR(64) PRIMARY KEY,
    template_id VARCHAR(64),
    inspection_type VARCHAR(24) NOT NULL,
    status VARCHAR(24) NOT NULL DEFAULT 'pending',
    title VARCHAR(255) NOT NULL,
    schedule_mode VARCHAR(16) NOT NULL DEFAULT 'manual',
    route_points JSONB NOT NULL DEFAULT '[]'::jsonb,
    assignee_id VARCHAR(64),
    scheduled_date DATE,
    due_date DATE,
    started_at TIMESTAMPTZ,
    completed_at TIMESTAMPTZ,
    work_order_id VARCHAR(64),
    project_id VARCHAR(64),
    space_id VARCHAR(64),
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT chk_fitask_type CHECK (inspection_type IN ('security','fire','facility','environment')),
    CONSTRAINT chk_fitask_status CHECK (status IN
        ('pending','in_progress','normal','abnormal','rectifying','rechecked','closed')),
    CONSTRAINT chk_fitask_schedule CHECK (schedule_mode IN ('template','manual','route'))
);
CREATE UNIQUE INDEX IF NOT EXISTS uq_fitask_template_date
    ON facility_inspection_tasks (template_id, scheduled_date)
    WHERE template_id IS NOT NULL AND scheduled_date IS NOT NULL;
CREATE INDEX IF NOT EXISTS idx_fitask_status ON facility_inspection_tasks (status);
CREATE INDEX IF NOT EXISTS idx_fitask_assignee ON facility_inspection_tasks (assignee_id);
CREATE INDEX IF NOT EXISTS idx_fitask_scheduled ON facility_inspection_tasks (scheduled_date, due_date);
CREATE INDEX IF NOT EXISTS idx_fitask_project_space ON facility_inspection_tasks (project_id, space_id);
CREATE INDEX IF NOT EXISTS idx_fitask_work_order ON facility_inspection_tasks (work_order_id);

CREATE TABLE IF NOT EXISTS facility_inspection_records (
    id VARCHAR(64) PRIMARY KEY,
    task_id VARCHAR(64) NOT NULL,
    result VARCHAR(16) NOT NULL,
    photos JSONB NOT NULL DEFAULT '[]'::jsonb,
    qr_code VARCHAR(255),
    gps_lat NUMERIC(10,7),
    gps_lng NUMERIC(10,7),
    checklist_result JSONB NOT NULL DEFAULT '[]'::jsonb,
    note TEXT,
    abnormal_note TEXT,
    inspector_id VARCHAR(64),
    recorded_at TIMESTAMPTZ NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT chk_firec_result CHECK (result IN ('normal','abnormal'))
);
CREATE INDEX IF NOT EXISTS idx_firec_task ON facility_inspection_records (task_id);
CREATE INDEX IF NOT EXISTS idx_firec_inspector ON facility_inspection_records (inspector_id);
CREATE INDEX IF NOT EXISTS idx_firec_recorded ON facility_inspection_records (recorded_at);

INSERT INTO permissions (permission_code, permission_name, domain, risk_level, requires_approval, status)
VALUES ('facility.inspection.manage', 'Manage building facility inspections', 'platform', 'medium', FALSE, 'active')
ON CONFLICT (permission_code) DO UPDATE SET permission_name = EXCLUDED.permission_name, status = 'active';
INSERT INTO role_permissions (role_id, permission_code)
SELECT 'role_platform_admin', 'facility.inspection.manage'
ON CONFLICT (role_id, permission_code) DO NOTHING;
