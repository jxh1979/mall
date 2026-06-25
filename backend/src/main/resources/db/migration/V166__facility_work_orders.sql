-- V166 楼宇维保/报修工单（③ 楼宇运营 S2，依赖 S1 设备台账）：报修建单→派单→流转，按 priority 自动算 sla_due_at。
-- 仿 V022/V120 merchant_service_work_orders 的 SLA 思路，但收敛为单权限 facility.work_order.manage、
-- 状态机 open→assigned→in_progress→resolved→closed；超期(overdue)按 sla_due_at 在读时派生，不落 breach 标记/升级 Job（YAGNI）。

CREATE TABLE IF NOT EXISTS facility_work_orders (
    id VARCHAR(64) PRIMARY KEY,
    equipment_id VARCHAR(64) REFERENCES facility_equipment(id),  -- 可空：非设备类报修（如公共区域）
    space_id VARCHAR(64),
    title VARCHAR(255) NOT NULL,
    description VARCHAR(2000),
    category VARCHAR(32) NOT NULL,                  -- repair|maintenance|inspection_followup
    priority VARCHAR(16) NOT NULL DEFAULT 'medium', -- low|medium|high|critical
    status VARCHAR(32) NOT NULL DEFAULT 'open',     -- open|assigned|in_progress|resolved|closed
    assignee VARCHAR(64),
    reported_by VARCHAR(64),
    sla_due_at TIMESTAMPTZ,                          -- 建单时按 priority 推算（critical 4h/high 24h/medium 72h/low 168h）
    resolved_at TIMESTAMPTZ,
    project_id VARCHAR(64),
    merchant_id VARCHAR(64),
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT chk_facility_wo_category CHECK (category IN ('repair', 'maintenance', 'inspection_followup')),
    CONSTRAINT chk_facility_wo_priority CHECK (priority IN ('low', 'medium', 'high', 'critical')),
    CONSTRAINT chk_facility_wo_status CHECK (status IN ('open', 'assigned', 'in_progress', 'resolved', 'closed'))
);
CREATE INDEX IF NOT EXISTS idx_facility_wo_status ON facility_work_orders (status, priority, created_at DESC);
CREATE INDEX IF NOT EXISTS idx_facility_wo_equipment ON facility_work_orders (equipment_id) WHERE equipment_id IS NOT NULL;
-- 支撑「超期未完成」查询：仅索引未完结且有 SLA 的行。
CREATE INDEX IF NOT EXISTS idx_facility_wo_sla_open ON facility_work_orders (sla_due_at)
 WHERE sla_due_at IS NOT NULL AND status NOT IN ('resolved', 'closed');

INSERT INTO permissions (permission_code, permission_name, domain, risk_level, requires_approval, status)
VALUES
    ('facility.work_order.manage', 'Manage building facility maintenance work orders', 'platform', 'high', FALSE, 'active')
ON CONFLICT (permission_code) DO UPDATE SET
    permission_name = EXCLUDED.permission_name, domain = EXCLUDED.domain,
    risk_level = EXCLUDED.risk_level, requires_approval = EXCLUDED.requires_approval, status = EXCLUDED.status;

INSERT INTO role_permissions (role_id, permission_code)
SELECT 'role_platform_admin', permission_code FROM permissions
 WHERE permission_code IN ('facility.work_order.manage')
ON CONFLICT (role_id, permission_code) DO NOTHING;
