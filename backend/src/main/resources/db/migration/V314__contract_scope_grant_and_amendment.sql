-- V314 超范围下单申请审批 + 变更子合同（2026-07-19 用户拍板 #30/#31）
-- #30：超范围下单不再当场拦死，可提交 scope_grant 申请走既有合同审批链，通过则授予该商户该品类临时/永久经营许可。
-- #31：同商户仅变更经营位置/面积 → 基于原合同开变更子合同（rent_terms.parentContractId 承载继承关系），原合同置 amended。
-- 零新表/新列：scope_grant/location_change 复用 contract_changes；子合同父指针(parentContractId)、变更类型(amendmentType)、
--   押金调整(depositAdjustment) 均留 rent_terms(JSONB)，与既有 allowedCategories/businessScope 同源同表，故无需新 GRANT。

-- 变更类型词表扩容：+scope_grant（超范围经营申请）+location_change（经营位置变更子合同）
ALTER TABLE contract_changes DROP CONSTRAINT IF EXISTS chk_contract_changes_type;
ALTER TABLE contract_changes ADD CONSTRAINT chk_contract_changes_type CHECK (change_type IN
    ('renewal', 'move_out', 'area_change', 'rent_change', 'business_scope_change', 'early_termination',
     'scope_grant', 'location_change'));

-- 合同状态 +amended（被变更子合同接替的原合同；非生效，自动退出计费/到期/续约作业与经营范围拦截生效谓词）
ALTER TABLE contracts DROP CONSTRAINT IF EXISTS chk_contracts_lifecycle_status;
ALTER TABLE contracts ADD CONSTRAINT chk_contracts_lifecycle_status CHECK (lifecycle_status IN
    ('drafting', 'pending_confirmation', 'pending_effective', 'effective', 'renewing',
     'expired', 'terminated', 'archived', 'amended'));

-- 新权限：商户/运营发起超范围经营申请（审批复用 contract.approve.* 既有分组并行会签链，不新增审批基建）
INSERT INTO permissions (permission_code, permission_name, domain, risk_level, requires_approval, status)
VALUES ('contract.scope_request.raise', 'Raise over-scope operating application', 'platform', 'medium', FALSE, 'active')
ON CONFLICT (permission_code) DO UPDATE SET
    permission_name = EXCLUDED.permission_name, domain = EXCLUDED.domain,
    risk_level = EXCLUDED.risk_level, requires_approval = EXCLUDED.requires_approval, status = EXCLUDED.status;

INSERT INTO role_permissions (role_id, permission_code)
VALUES ('role_platform_admin', 'contract.scope_request.raise')
ON CONFLICT (role_id, permission_code) DO NOTHING;
