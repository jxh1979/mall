-- 文件版本：V0.01
-- HP-C01-R1 合同审批流程模板按三类配置（配置实体）。依据：BSM L1348「合同的新建、到期终止、变更分别使用各自配置的审批流程模板，三类均遵守同一
-- 全平台审批模型，各模板的阶段与岗位可以不同——发起时指定这条主线的最终审批人（默认总经理）……业务审核、财务审核、法务审核作为平行的相关部门线」；
-- PGM L536「三类各走各自的审批流程模板，模板分别可配」「任一审批节点超过后台配置的时限（业务参数，按合同类型可配，出厂默认 3 个工作日，须同时配置上下限）」；
-- ENUM L255「按新建、到期终止、变更三类分别配置流程模板」；裁决 Q23「保留三类模板……不是把三类合成同一审批链」；
-- DEC-R5-2026-08-04-06「此前合同类……同阶段并行会签的表述一律作废」。

-- ① 模板实体：contract_approval_flows 按 (综合体, 合同类型) 一条生效模板（project_id 空 = 平台默认）。
--    模板字段 = 平行相关部门线（approval_lines.line_code）+ 主线默认最终审批人（空 = 后台配置或发起时指定）。
ALTER TABLE contract_approval_flows
    ADD COLUMN IF NOT EXISTS contract_type VARCHAR(32) NOT NULL DEFAULT 'new',
    ADD COLUMN IF NOT EXISTS required_line_codes JSONB NOT NULL
        DEFAULT '["business_review","finance_review","legal_review"]'::jsonb,
    ADD COLUMN IF NOT EXISTS default_final_approver_id VARCHAR(64) REFERENCES access_users(id) ON DELETE SET NULL;
ALTER TABLE contract_approval_flows
    ADD CONSTRAINT chk_contract_approval_flows_type CHECK (contract_type IN ('new', 'expiry_termination', 'change'));
DROP INDEX IF EXISTS uq_contract_approval_flows_project_active;
CREATE UNIQUE INDEX IF NOT EXISTS uq_contract_approval_flows_project_type_active
    ON contract_approval_flows (COALESCE(project_id, '__default__'), contract_type) WHERE active;

UPDATE contract_approval_flows SET name = '平台默认·新建', updated_at = now() WHERE id = 'caf_default';
INSERT INTO contract_approval_flows (id, project_id, contract_type, name, active)
VALUES
    ('caf_default_expiry_termination', NULL, 'expiry_termination', '平台默认·到期终止', TRUE),
    ('caf_default_change',             NULL, 'change',             '平台默认·变更',     TRUE)
ON CONFLICT (id) DO NOTHING;

-- ② 旧「阶段步」配置表删除（模板的岗位/线落在列上）；合同与变更预留统一审批模型复核单接入位（与 V485 purchase_quantity_resolutions.review_id 同形）。
DROP TABLE IF EXISTS contract_approval_flow_steps;
ALTER TABLE contracts ADD COLUMN IF NOT EXISTS review_id VARCHAR(80) REFERENCES supervisor_review_decisions(review_id);
ALTER TABLE contract_changes ADD COLUMN IF NOT EXISTS review_id VARCHAR(80) REFERENCES supervisor_review_decisions(review_id);

-- ③ 业务审核线（与 V391 财务/税务/法务线同形：出厂最低起点 1，起点人由平台管理员按实际组织填写）。
INSERT INTO approval_lines (line_code, line_name, min_start_rank, entry_user_id, status)
VALUES ('business_review', '业务审核线', 1, NULL, 'active')
ON CONFLICT (line_code) DO NOTHING;

-- ④ 三类合同审批时限策略（PGM L536：按合同类型可配，出厂默认 3 个工作日，上下限 1..30；与 V389 同一参数形）。
INSERT INTO supervisor_review_policies
    (policy_code, action_type, object_type, trigger_mode, outcome, approval_level, required_level, parameters)
VALUES
    ('contract_new',                'contract.new',                'contract',            'always', 'block', 'one_level_up', 1,
        '{"deadlineWorkingDays":{"value":3,"min":1,"max":30}}'),
    ('contract_expiry_termination', 'contract.expiry_termination', 'contract_settlement', 'always', 'block', 'one_level_up', 1,
        '{"deadlineWorkingDays":{"value":3,"min":1,"max":30}}'),
    ('contract_change',             'contract.change',             'contract_change',     'always', 'block', 'one_level_up', 1,
        '{"deadlineWorkingDays":{"value":3,"min":1,"max":30}}')
ON CONFLICT (policy_code) DO NOTHING;
