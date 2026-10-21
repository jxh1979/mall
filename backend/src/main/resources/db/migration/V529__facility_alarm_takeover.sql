-- 文件版本：V0.01
-- HP-F01（BSM「楼宇运营」告警条 L1417；2026-10-21 操作员裁决补充第 1 条）：设备告警落接警单并按「告警类型×优先级」计响应时限；
-- 接警值守安排（主接、备用、物业主管、项目负责人、专业处置人员及联系方式）挂已发布班次；首次派发逐次留痕；
-- 明确接手是唯一改变接警状态的动作，送达、已读、接听都不改接警状态。
-- 告警类型＝设备四组台账（电梯扶梯/暖通空调/配电供电/给排水消防），优先级＝紧急/高/普通（上报人或物联网当场选）；
-- 响应时限由后台逐格配置，不预置出厂数值。

CREATE TABLE facility_alarm_response_policies (
    alarm_type VARCHAR(32) NOT NULL,
    priority VARCHAR(16) NOT NULL,
    response_minutes INTEGER NOT NULL,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    PRIMARY KEY (alarm_type, priority),
    CONSTRAINT chk_fac_alarm_policy_type CHECK (alarm_type IN ('elevator','hvac','power','water_fire')),
    CONSTRAINT chk_fac_alarm_policy_priority CHECK (priority IN ('urgent','high','normal')),
    CONSTRAINT chk_fac_alarm_policy_minutes CHECK (response_minutes > 0)
);

CREATE TABLE facility_alarms (
    id VARCHAR(64) PRIMARY KEY,
    equipment_id VARCHAR(64) NOT NULL REFERENCES facility_equipment(id),
    work_order_id VARCHAR(64) NOT NULL REFERENCES facility_work_orders(id),
    project_id VARCHAR(64),
    alarm_type VARCHAR(32) NOT NULL,
    priority VARCHAR(16) NOT NULL,
    alarm_source VARCHAR(16),
    note TEXT,
    status VARCHAR(24) NOT NULL DEFAULT 'awaiting_takeover',
    raised_at TIMESTAMPTZ NOT NULL,
    response_due_at TIMESTAMPTZ NOT NULL,
    escalation_round INTEGER NOT NULL DEFAULT 0,
    fallback_to_backup_at TIMESTAMPTZ,
    holder_user_id VARCHAR(64),
    holder_shift_id VARCHAR(64) REFERENCES staff_shift_schedules(id),
    holder_shift_end_at TIMESTAMPTZ,
    taken_over_at TIMESTAMPTZ,
    uncovered BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT chk_fac_alarm_type CHECK (alarm_type IN ('elevator','hvac','power','water_fire')),
    CONSTRAINT chk_fac_alarm_priority CHECK (priority IN ('urgent','high','normal')),
    CONSTRAINT chk_fac_alarm_source CHECK (alarm_source IS NULL OR alarm_source IN ('manual','iot')),
    CONSTRAINT chk_fac_alarm_status CHECK (status IN ('awaiting_takeover','taken_over')),
    CONSTRAINT chk_fac_alarm_holder CHECK (status <> 'taken_over'
        OR (holder_user_id IS NOT NULL AND holder_shift_id IS NOT NULL AND taken_over_at IS NOT NULL))
);
CREATE INDEX idx_facility_alarms_due ON facility_alarms (status, response_due_at);
CREATE INDEX idx_facility_alarms_project ON facility_alarms (project_id, status);

CREATE TABLE facility_alarm_duty_assignments (
    id VARCHAR(64) PRIMARY KEY,
    project_id VARCHAR(64) NOT NULL REFERENCES projects(id),
    shift_id VARCHAR(64) NOT NULL REFERENCES staff_shift_schedules(id),
    duty_role VARCHAR(24) NOT NULL,
    contact_phone VARCHAR(32) NOT NULL,
    created_by VARCHAR(64) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    cancelled_by VARCHAR(64),
    cancelled_at TIMESTAMPTZ,
    cancel_reason TEXT,
    CONSTRAINT chk_fac_alarm_duty_role CHECK (duty_role IN ('primary','backup','supervisor','project_lead','specialist')),
    CONSTRAINT chk_fac_alarm_duty_cancel CHECK (cancelled_at IS NULL OR (cancelled_by IS NOT NULL AND cancel_reason IS NOT NULL))
);
CREATE UNIQUE INDEX uq_fac_alarm_duty_shift_role ON facility_alarm_duty_assignments (shift_id, duty_role)
    WHERE cancelled_at IS NULL;
CREATE INDEX idx_fac_alarm_duty_project ON facility_alarm_duty_assignments (project_id) WHERE cancelled_at IS NULL;

-- 每次系统消息、短信、语音呼叫各记一行，outcome 取通道真实结果；送达、接听都不等于明确接手。
CREATE TABLE facility_alarm_contact_attempts (
    id VARCHAR(64) PRIMARY KEY,
    alarm_id VARCHAR(64) NOT NULL REFERENCES facility_alarms(id),
    round INTEGER NOT NULL,
    user_id VARCHAR(64) NOT NULL,
    duty_role VARCHAR(24) NOT NULL,
    channel VARCHAR(16) NOT NULL,
    outcome VARCHAR(24) NOT NULL,
    provider_ref VARCHAR(128),
    reason TEXT,
    attempted_at TIMESTAMPTZ NOT NULL,
    CONSTRAINT chk_fac_alarm_attempt_role CHECK (duty_role IN
        ('primary','backup','supervisor','project_lead','specialist','equipment_owner')),
    CONSTRAINT chk_fac_alarm_attempt_channel CHECK (channel IN ('system_message','sms','voice')),
    CONSTRAINT chk_fac_alarm_attempt_outcome CHECK (outcome IN
        ('sent','failed','throttled','skipped_switch_off','placed','answered')),
    CONSTRAINT uq_fac_alarm_attempt UNIQUE (alarm_id, round, user_id, duty_role, channel)
);

CREATE TABLE facility_alarm_takeovers (
    id VARCHAR(64) PRIMARY KEY,
    alarm_id VARCHAR(64) NOT NULL REFERENCES facility_alarms(id),
    user_id VARCHAR(64) NOT NULL,
    shift_id VARCHAR(64) NOT NULL REFERENCES staff_shift_schedules(id),
    duty_role VARCHAR(24) NOT NULL,
    kind VARCHAR(16) NOT NULL,
    note TEXT,
    taken_over_at TIMESTAMPTZ NOT NULL,
    CONSTRAINT chk_fac_alarm_takeover_role CHECK (duty_role IN ('primary','backup','supervisor','project_lead')),
    CONSTRAINT chk_fac_alarm_takeover_kind CHECK (kind IN ('first','handover'))
);
CREATE INDEX idx_fac_alarm_takeovers_alarm ON facility_alarm_takeovers (alarm_id);

-- 权限：后台管理（响应时限、值守安排、接警记录）与明确接手；授予现持设施工单管理权的角色与权限模板。
INSERT INTO permissions (permission_code, permission_name, domain, risk_level, requires_approval, status)
VALUES
    ('facility.alarm.manage', '管理设备告警接警（响应时限、值守安排、接警记录）', 'facility', 'medium', FALSE, 'active'),
    ('facility.alarm.take_over', '设备告警明确接手', 'facility', 'medium', FALSE, 'active')
ON CONFLICT (permission_code) DO NOTHING;

INSERT INTO role_permissions (role_id, permission_code)
SELECT DISTINCT rp.role_id, p.code
  FROM role_permissions rp
 CROSS JOIN (VALUES ('facility.alarm.manage'), ('facility.alarm.take_over')) AS p(code)
 WHERE rp.permission_code = 'facility.work_order.manage'
ON CONFLICT (role_id, permission_code) DO NOTHING;

INSERT INTO permission_template_permissions (template_id, permission_code)
SELECT DISTINCT tp.template_id, p.code
  FROM permission_template_permissions tp
 CROSS JOIN (VALUES ('facility.alarm.manage'), ('facility.alarm.take_over')) AS p(code)
 WHERE tp.permission_code = 'facility.work_order.manage'
ON CONFLICT DO NOTHING;
