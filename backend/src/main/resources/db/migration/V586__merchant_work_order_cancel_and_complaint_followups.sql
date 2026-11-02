-- 文件版本：V0.01
-- V586（第十八批 M 段第 111 条、第 118 条共用这一份；111-R2、111-R3 先建，111-R4、118-R1 往本文件续写）。

-- 111-R2（第十八批裁决 27，2026-11-01 操作员选「两者都有」：「派单前商户自己取消；派单后，重复提或误提的由客户关怀部填原因取消」；
-- ENUM:264「商户服务工单状态｜……已关闭、已取消（商户专区发起；……）」）：状态 CHECK（V022）早有 cancelled，动作流水补取消动作，原因写在动作说明。
ALTER TABLE merchant_service_work_order_actions
    DROP CONSTRAINT IF EXISTS chk_merchant_service_work_order_action_type;

ALTER TABLE merchant_service_work_order_actions
    ADD CONSTRAINT chk_merchant_service_work_order_action_type
    CHECK (action_type IN ('created', 'assigned', 'commented', 'status_changed', 'closed', 'escalated', 'cancelled'));

-- 111-R3（BSM:1385「客诉须经客户关怀部确认后方可视为形成闭环，未经其确认的一律不算闭环」；PGM:502「……确认闭环，并负责工单处理」；
-- DEC-R4-2026-08-04-12）：客诉跟进，物业工单链（客人投诉与员工新建的投诉）与商户服务工单链（商户投诉）共用，每张投诉单至多一行；
-- 关单本身不算闭环，客户关怀部对已关闭的投诉单独确认闭环。complaint_class、classified_by、classified_at 供 118-R1 客户关怀部认定投诉类别。
CREATE TABLE IF NOT EXISTS complaint_followups (
    chain VARCHAR(24) NOT NULL,
    work_order_id VARCHAR(64) NOT NULL,
    complaint_class VARCHAR(64),
    classified_by VARCHAR(64),
    classified_at TIMESTAMPTZ,
    loop_confirmed_by VARCHAR(64),
    loop_confirmed_at TIMESTAMPTZ,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    PRIMARY KEY (chain, work_order_id),
    CONSTRAINT chk_complaint_followup_chain CHECK (chain IN ('facility', 'merchant_service'))
);

COMMENT ON TABLE complaint_followups IS '客诉跟进：客户关怀部确认闭环与认定投诉类别（物业工单链、商户服务工单链共用；第 111 条 111-R3、第 118 条）';

-- 111-R4（第十八批裁决 28，2026-11-01 操作员选「客人投诉时自选门店」：「客人投诉时自己选涉及的门店（选填），客户关怀部可以更正；
-- 标了门店的客人投诉，加上该店自己在商户专区提的投诉，进该店周报；全部客诉按所在项目进各公司周报」；BSM:1385「系统每周自动生成客诉周报，
-- 列明当周客诉的笔数、类型、处理进度与闭环情况——店内的报送该店主理人与店长，各公司的报送该公司最高负责人与平台最高负责人」）：
-- 物业工单链的客人投诉记涉及门店（选填；商户服务工单的门店就是提单商户，不另记）。
ALTER TABLE complaint_followups ADD COLUMN IF NOT EXISTS store_merchant_id VARCHAR(64);

COMMENT ON COLUMN complaint_followups.store_merchant_id IS '涉及门店（物业工单链的客人投诉：客人投诉时自选，客户关怀部可更正；第十八批裁决 28）';

-- 周报收件人照「按持码取人」范式：运营把本码授给主理人、店长（商户账号，收本店周报）与公司最高负责人、平台最高负责人（平台员工，收可触及项目的周报）。
INSERT INTO permissions (permission_code, permission_name, domain, risk_level, requires_approval, status)
VALUES ('complaint.weekly_report.receive', '接收客诉周报', 'platform', 'medium', FALSE, 'active')
ON CONFLICT (permission_code) DO NOTHING;
INSERT INTO role_permissions (role_id, permission_code)
VALUES ('role_platform_admin', 'complaint.weekly_report.receive')
ON CONFLICT (role_id, permission_code) DO NOTHING;

-- 118-R1（BSM:1385「一个月内，同一类投诉由三个不同的人分别提出的，立即上升到项目负责人、娱乐平台负责人与公司最高负责人；
-- 是否属于同一类由客户关怀部认定」；DEC-R4-2026-08-04-12）：上升收件人同样按持码取，运营把本码授给项目负责人、娱乐平台负责人与公司最高负责人。
INSERT INTO permissions (permission_code, permission_name, domain, risk_level, requires_approval, status)
VALUES ('complaint.escalation.receive', '接收同类投诉上升通知', 'platform', 'medium', FALSE, 'active')
ON CONFLICT (permission_code) DO NOTHING;
INSERT INTO role_permissions (role_id, permission_code)
VALUES ('role_platform_admin', 'complaint.escalation.receive')
ON CONFLICT (role_id, permission_code) DO NOTHING;
