-- V166 维保/物业工单（③楼宇运营 S2，据 M1§4 + 速查表§十重建）。
-- 类型6(报修/保洁/投诉/整改/巡检/其他)、状态9、SLA「类型×优先级」后台可配(非硬编码)、
-- 设备告警/故障→自动转工单+维修完回写设备状态、验收按类型配置、满意度1-5、商户自助提单。
-- 收口：旧 V022 merchant_service_work_orders 由本表统一替代（死表，停止写入；不在此 DROP 以免动历史数据，后续数据迁移后再废）。

CREATE TABLE IF NOT EXISTS facility_work_order_policy (
    id VARCHAR(64) PRIMARY KEY,
    work_order_type VARCHAR(24) NOT NULL,           -- repair|cleaning|complaint|rectification|inspection|other
    priority VARCHAR(16) NOT NULL,                  -- urgent|high|normal
    response_minutes INTEGER NOT NULL,              -- 响应时限（分钟）
    handle_minutes INTEGER NOT NULL,                -- 处理时限（分钟）
    requires_acceptance BOOLEAN NOT NULL DEFAULT FALSE,  -- 是否需验收（简单类否/复杂类是）
    acceptance_by VARCHAR(16) NOT NULL DEFAULT 'handler', -- handler(完成即结)|supervisor(主管验收)|reporter(提单方确认)
    active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT chk_fwop_type CHECK (work_order_type IN ('repair','cleaning','complaint','rectification','inspection','other')),
    CONSTRAINT chk_fwop_priority CHECK (priority IN ('urgent','high','normal')),
    CONSTRAINT chk_fwop_accept_by CHECK (acceptance_by IN ('handler','supervisor','reporter')),
    CONSTRAINT uq_fwop_type_priority UNIQUE (work_order_type, priority)
);

CREATE TABLE IF NOT EXISTS facility_work_orders (
    id VARCHAR(64) PRIMARY KEY,
    code VARCHAR(40) NOT NULL UNIQUE,
    work_order_type VARCHAR(24) NOT NULL,
    priority VARCHAR(16) NOT NULL DEFAULT 'normal',
    status VARCHAR(24) NOT NULL DEFAULT 'pending_assign', -- 9态见 CHECK
    title VARCHAR(255) NOT NULL,
    description TEXT,
    source VARCHAR(24) NOT NULL DEFAULT 'platform',       -- merchant_self|platform|equipment_alarm|inspection
    space_id VARCHAR(64),
    equipment_id VARCHAR(64),                             -- 设备告警转单时关联（回写设备状态）
    reporter_id VARCHAR(64),
    reporter_type VARCHAR(24),                            -- merchant|staff|system
    assignee_id VARCHAR(64),
    response_due_at TIMESTAMPTZ,
    handle_due_at TIMESTAMPTZ,
    assigned_at TIMESTAMPTZ,
    responded_at TIMESTAMPTZ,
    completed_at TIMESTAMPTZ,
    verified_at TIMESTAMPTZ,
    verifier_id VARCHAR(64),
    satisfaction SMALLINT,                                -- 1-5（回访/验收时提单方评分）
    overtime BOOLEAN NOT NULL DEFAULT FALSE,              -- 超时标记（超 due 自动置，升级通知主管）
    resolution TEXT,
    project_id VARCHAR(64),
    merchant_id VARCHAR(64),
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT chk_fwo_type CHECK (work_order_type IN ('repair','cleaning','complaint','rectification','inspection','other')),
    CONSTRAINT chk_fwo_priority CHECK (priority IN ('urgent','high','normal')),
    CONSTRAINT chk_fwo_source CHECK (source IN ('merchant_self','platform','equipment_alarm','inspection')),
    CONSTRAINT chk_fwo_satisfaction CHECK (satisfaction IS NULL OR satisfaction BETWEEN 1 AND 5),
    CONSTRAINT chk_fwo_status CHECK (status IN
        ('pending_assign','assigned','processing','pending_review','pending_acceptance','accepted','closed','suspended','overtime'))
);
CREATE INDEX IF NOT EXISTS idx_fwo_status ON facility_work_orders (status);
CREATE INDEX IF NOT EXISTS idx_fwo_equipment ON facility_work_orders (equipment_id);
CREATE INDEX IF NOT EXISTS idx_fwo_assignee ON facility_work_orders (assignee_id);

-- 默认 SLA×验收策略种子（后台可改）：报修/整改=复杂需主管验收；保洁/投诉=简单完成即结。
INSERT INTO facility_work_order_policy (id, work_order_type, priority, response_minutes, handle_minutes, requires_acceptance, acceptance_by) VALUES
    ('fwop_repair_urgent','repair','urgent',30,240,TRUE,'supervisor'),
    ('fwop_repair_high','repair','high',60,720,TRUE,'supervisor'),
    ('fwop_repair_normal','repair','normal',120,1440,TRUE,'supervisor'),
    ('fwop_rectification_high','rectification','high',60,1440,TRUE,'supervisor'),
    ('fwop_rectification_normal','rectification','normal',240,4320,TRUE,'supervisor'),
    ('fwop_cleaning_normal','cleaning','normal',120,480,FALSE,'handler'),
    ('fwop_complaint_high','complaint','high',60,720,FALSE,'reporter'),
    ('fwop_complaint_normal','complaint','normal',120,1440,FALSE,'reporter'),
    ('fwop_inspection_normal','inspection','normal',240,2880,FALSE,'handler'),
    ('fwop_other_normal','other','normal',240,2880,FALSE,'handler')
ON CONFLICT (id) DO NOTHING;

INSERT INTO permissions (permission_code, permission_name, domain, risk_level, requires_approval, status)
VALUES ('facility.work_order.manage', 'Manage building facility work orders', 'platform', 'medium', FALSE, 'active')
ON CONFLICT (permission_code) DO UPDATE SET permission_name = EXCLUDED.permission_name, status = 'active';
INSERT INTO role_permissions (role_id, permission_code)
SELECT 'role_platform_admin', 'facility.work_order.manage'
ON CONFLICT (role_id, permission_code) DO NOTHING;
