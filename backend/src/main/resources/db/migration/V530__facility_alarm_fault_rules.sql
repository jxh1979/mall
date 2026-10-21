-- 文件版本：V0.01
-- HP-F01 R7（BSM「楼宇运营」告警条 L1417「各类故障处置规则先由设备专业负责人和维保单位确认，发生告警时按已确认规则处理；
-- 未覆盖的转当班专业处置人员判定。确定停用或继续使用等处置要求，不等于授权平台直接远程停机」；决策台账 Q17D）：
-- 故障处置规则须专业负责人与维保单位两方确认才生效；告警落单时无生效规则覆盖即标未覆盖，由当班专业处置人员判定；
-- 接警单只记录所按规则或判定的处置要求，不调用任何设备控制。

CREATE TABLE facility_fault_handling_rules (
    id VARCHAR(64) PRIMARY KEY,
    project_id VARCHAR(64) NOT NULL REFERENCES projects(id),
    equipment_id VARCHAR(64) REFERENCES facility_equipment(id),
    equipment_type VARCHAR(32) NOT NULL,
    fault_scenario TEXT NOT NULL,
    handling_requirement TEXT NOT NULL,
    status VARCHAR(24) NOT NULL DEFAULT 'pending_confirmation',
    specialist_confirmed_by VARCHAR(64),
    specialist_confirmed_at TIMESTAMPTZ,
    vendor_confirmed_by VARCHAR(64),
    vendor_confirmed_at TIMESTAMPTZ,
    created_by VARCHAR(64) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT chk_fac_fault_rule_type CHECK (equipment_type IN ('elevator','hvac','power','water_fire')),
    CONSTRAINT chk_fac_fault_rule_status CHECK (status IN ('pending_confirmation','active','retired')),
    CONSTRAINT chk_fac_fault_rule_active CHECK (status <> 'active'
        OR (specialist_confirmed_at IS NOT NULL AND vendor_confirmed_at IS NOT NULL))
);
CREATE INDEX idx_fac_fault_rules_active ON facility_fault_handling_rules (project_id, equipment_type) WHERE status = 'active';

ALTER TABLE facility_alarms
    ADD COLUMN applied_rule_id VARCHAR(64) REFERENCES facility_fault_handling_rules(id),
    ADD COLUMN disposition_note TEXT,
    ADD COLUMN disposition_by VARCHAR(64),
    ADD COLUMN disposition_at TIMESTAMPTZ;

-- 故障处置规则确认：授予现持设施工单管理权的角色与权限模板（设备专业负责人一侧）；维保单位一侧按 V181 外包限时授权范式
-- 只进外包维保模板 tpl_outsourced_facility（本项目范围），不给外包角色直挂运行时权限。
INSERT INTO permissions (permission_code, permission_name, domain, risk_level, requires_approval, status)
VALUES ('facility.fault_rule.confirm', '确认设备故障处置规则', 'facility', 'medium', FALSE, 'active')
ON CONFLICT (permission_code) DO NOTHING;

INSERT INTO role_permissions (role_id, permission_code)
SELECT DISTINCT role_id, 'facility.fault_rule.confirm'
  FROM role_permissions
 WHERE permission_code = 'facility.work_order.manage'
ON CONFLICT (role_id, permission_code) DO NOTHING;

INSERT INTO permission_template_items (template_id, permission_code, effect, data_scope, sensitive)
VALUES ('tpl_outsourced_facility', 'facility.fault_rule.confirm', 'grant', 'project', FALSE)
ON CONFLICT (template_id, permission_code, effect) DO NOTHING;

INSERT INTO permission_template_permissions (template_id, permission_code)
SELECT DISTINCT template_id, 'facility.fault_rule.confirm'
  FROM permission_template_permissions
 WHERE permission_code = 'facility.work_order.manage'
ON CONFLICT DO NOTHING;
