-- V194 需求：客人账号四态生命周期（active/suspended/blacklisted/deregistered）功能落地。
-- 加临时封禁到期日 + 状态变更留痕字段 + 平台管理客人账号状态权限。
ALTER TABLE customers ADD COLUMN IF NOT EXISTS suspended_until TIMESTAMPTZ;   -- 临时封禁(suspended)到期日；到期自动恢复 active；null=非临时封禁

-- 平台管理客人账号生命周期（封禁/拉黑/恢复）的权限点（高危）。客人自助注销无需此权限（凭本人 token）。
INSERT INTO permissions (permission_code, permission_name, domain, risk_level, requires_approval, status)
VALUES ('customer.account.manage', 'Manage customer account lifecycle (suspend/blacklist/reinstate)', 'customer', 'high', FALSE, 'active')
ON CONFLICT (permission_code) DO NOTHING;
