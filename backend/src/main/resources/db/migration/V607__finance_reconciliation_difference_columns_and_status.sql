-- 文件版本：V0.01
-- 第十八批 Y 段：财务对账差异 finance_reconciliation_differences 的列与状态约束（180-R2、181-R1、181-R2 按顺序往本文件追加）。
-- 研发阶段无存量（UD-07），不做数据迁移。

-- 180-R2：渠道对账单原件附件编号随差异落库。
-- 依据：ENUM:202「渠道接入前的人工录入必须同时上传渠道提供的对账单原件作为凭证，复核人须比对原件与录入值后签字；
--   无原件的录入不得进入对账结论，只能挂为待补凭证。」——不带原件为空数组（待补凭证）。
ALTER TABLE finance_reconciliation_differences
    ADD COLUMN evidence_attachment_ids JSONB NOT NULL DEFAULT '[]'::jsonb;
ALTER TABLE finance_reconciliation_differences
    ADD CONSTRAINT chk_frd_evidence CHECK (jsonb_typeof(evidence_attachment_ids) = 'array');

-- 181-R1：差异处理四态与负责人、本阶段起始时刻、识别原因、复核单号；close_reason（V189）沿用为处理结论。
-- 依据：ENUM:191「差异处理状态 待识别、待处理、待复核、已关闭。……复核环节属业务审批，沿用平台治理地图「全平台审批通用时限」条。
--   各阶段由财务岗全程负责。」；BSM:1729「对不上的进差异处理，按原因定位、复核后处理」。
ALTER TABLE finance_reconciliation_differences
    ADD CONSTRAINT chk_frd_status CHECK (status IN ('pending_identification', 'pending_handling', 'pending_review', 'closed'));
ALTER TABLE finance_reconciliation_differences
    ADD COLUMN responsible_user_id VARCHAR(64),
    ADD COLUMN stage_entered_at TIMESTAMPTZ,
    ADD COLUMN identified_cause VARCHAR(1000),
    ADD COLUMN review_id VARCHAR(64);

-- 181-R1：识别、提交复核入口 @GuardedAction(capability = finance.reconciliation.resolve) 的跨端全局显式开启行（照 V595，说明用 V605 的中文说法；PGM:742 缺行即关）。
INSERT INTO terminal_capability_switches
    (id, terminal_type, capability_code, scope_type, scope_id, enabled, reason, created_at, updated_at)
VALUES
    ('tcs_optin_finance.reconciliation.resolve', '*', 'finance.reconciliation.resolve', 'global', 'global', TRUE,
     '已发布在用能力显式开启（平台治理地图第 742 行：缺行默认关闭，须显式打开）', now(), now())
ON CONFLICT (terminal_type, capability_code, scope_type, scope_id) DO NOTHING;

-- 181-R2：待识别、待处理两阶段停留时限（按项目可配）与超时升级。
-- 依据：ENUM:191「待识别与待处理两个阶段各自的停留时限为业务参数，按项目可配，出厂默认 1 个工作日，须同时配置上下限；超时未推进的，
--   系统自动提醒并升级到上一级负责人。」——差异两列记各阶段已超时升级的时刻（每阶段只升级一次；复核驳回退回待处理时清空待处理一列）。
--   时限表不预置行，项目没有行按出厂默认 1 个工作日；时限与上下限首次配置就须同时填写，不自拟上下限出厂数值（同 V591）。
ALTER TABLE finance_reconciliation_differences
    ADD COLUMN identify_escalated_at TIMESTAMPTZ,
    ADD COLUMN handle_escalated_at TIMESTAMPTZ;

CREATE TABLE finance_reconciliation_stage_limits (
    project_id VARCHAR(64) NOT NULL,
    stage VARCHAR(32) NOT NULL,
    working_days INTEGER NOT NULL,
    min_working_days INTEGER NOT NULL,
    max_working_days INTEGER NOT NULL,
    updated_by VARCHAR(128),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    PRIMARY KEY (project_id, stage),
    CONSTRAINT chk_frsl_stage CHECK (stage IN ('pending_identification', 'pending_handling')),
    CONSTRAINT chk_frsl_working_days CHECK (
        1 <= min_working_days AND min_working_days <= working_days AND working_days <= max_working_days)
);

-- 181-R2：停留时限配置权限挂平台管理员（服务层另按项目范围校验：项目账号只能改本权限可触及的项目，同 V591）。
INSERT INTO permissions (permission_code, permission_name, domain, risk_level, requires_approval, status)
VALUES ('finance.reconciliation.policy.manage', '配置对账差异停留时限（按项目）', 'finance', 'high', TRUE, 'active')
ON CONFLICT (permission_code) DO NOTHING;
INSERT INTO role_permissions (role_id, permission_code)
VALUES ('role_platform_admin', 'finance.reconciliation.policy.manage')
ON CONFLICT (role_id, permission_code) DO NOTHING;
