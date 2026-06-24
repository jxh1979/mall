-- V161 合同环地基（② 履约闭环 S1，PRD phase6）：合同「生效」终态 + contract_spaces 写入约束 + 生效管理权限。
-- contracts.lifecycle_status 现状无 CHECK（V010 仅 NOT NULL，dev 存量 0 行 → 加 CHECK 无违例风险）。
--   取值集：draft/approved/rejected/returned（现有 approveContract 产出）+ effective（本批 activate 产出）；expired/terminated 留 S3 扩。
-- contract_spaces(V080) 此前全仓零写入；S1 起补写入路径（合同生效时绑定所租空间），补 project_id 预留综合体维度（登记 domain-ownership）。

ALTER TABLE contract_spaces ADD COLUMN IF NOT EXISTS project_id VARCHAR(64);
CREATE INDEX IF NOT EXISTS idx_contract_spaces_contract ON contract_spaces (contract_id);

ALTER TABLE contracts
    ADD CONSTRAINT chk_contracts_lifecycle_status
    CHECK (lifecycle_status IN ('draft', 'approved', 'rejected', 'returned', 'effective'));

-- 合同生效管理权限（高敏：决定合同进入履约 + 绑定经营空间）。仿 V010 contract.* 种子范式，授 role_platform_admin。
INSERT INTO permissions (permission_code, permission_name, domain, risk_level, requires_approval, status)
VALUES
    ('contract.lifecycle.activate', 'Activate approved contract to effective and bind leased spaces', 'platform', 'high', TRUE, 'active')
ON CONFLICT (permission_code) DO UPDATE SET
    permission_name = EXCLUDED.permission_name,
    domain = EXCLUDED.domain,
    risk_level = EXCLUDED.risk_level,
    requires_approval = EXCLUDED.requires_approval,
    status = EXCLUDED.status;

INSERT INTO role_permissions (role_id, permission_code)
SELECT 'role_platform_admin', permission_code
  FROM permissions
 WHERE permission_code IN ('contract.lifecycle.activate')
ON CONFLICT (role_id, permission_code) DO NOTHING;
