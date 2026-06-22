-- 2e 权限码独立：approveSupplyContract 此前误用 inventory.stock.move（库存移动权限）审批供货合同——权限语义错配=越权
-- （持库存移动权者即可审批供货合同）。注册独立的合同审批权限 inventory.supply_contract.approve，
-- 与建合同的 inventory.supply_contract.manage（V085 已注册）分离，实现建/审批分权。
-- createWarehouse / createSupplyContract 改挂 V085 已注册的 inventory.warehouse.manage / inventory.supply_contract.manage（仅代码改挂，权限已存在）。
INSERT INTO permissions (permission_code, permission_name, domain, risk_level, requires_approval, status)
VALUES ('inventory.supply_contract.approve', 'Approve supply contracts', 'inventory', 'high', TRUE, 'active')
ON CONFLICT (permission_code) DO UPDATE SET
    permission_name = EXCLUDED.permission_name,
    domain = EXCLUDED.domain,
    risk_level = EXCLUDED.risk_level,
    requires_approval = EXCLUDED.requires_approval,
    status = EXCLUDED.status;

INSERT INTO role_permissions (role_id, permission_code)
SELECT 'role_platform_admin', 'inventory.supply_contract.approve'
ON CONFLICT (role_id, permission_code) DO NOTHING;
