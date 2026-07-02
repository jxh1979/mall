-- BI S3 活证据抓真缺口:三个代码运行时检查的权限码从未种入 permissions,
-- 导致合同无法激活(contract.lifecycle.activate)、通票权益无法读取/核销(bar.entitlement.*)。
-- 风险等级对齐兄弟码:激活/核销=high 且授予须审批;读取=low 免审批。
INSERT INTO permissions (permission_code, permission_name, domain, risk_level, requires_approval, status)
VALUES
    ('contract.lifecycle.activate', 'Activate contract (freeze snapshot, bind spaces, schedule bills)', 'platform', 'high', TRUE,  'active'),
    ('bar.entitlement.read',        'Read pass ticket entitlements at bar',                             'bar',      'low',  FALSE, 'active'),
    ('bar.entitlement.redeem',      'Redeem pass ticket entitlements at bar',                           'bar',      'high', TRUE,  'active')
ON CONFLICT (permission_code) DO NOTHING;

-- 平台管理员默认可用(与 V200 等 role_platform_admin 全授范式一致);业务岗位走治理装配闭环授予。
INSERT INTO role_permissions (role_id, permission_code)
SELECT 'role_platform_admin', permission_code
  FROM permissions
 WHERE permission_code IN ('contract.lifecycle.activate', 'bar.entitlement.read', 'bar.entitlement.redeem')
ON CONFLICT (role_id, permission_code) DO NOTHING;
