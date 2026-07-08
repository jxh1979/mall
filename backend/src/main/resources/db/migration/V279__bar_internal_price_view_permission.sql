-- V279: 内部结算价(成本)查看=独立高危权限。用户2026-07-08 拍板:内部结算价只有 web 后台有权限的人能看,
-- 店员 POS / 客人 / 一般后台用户都不可见(响应按权限脱敏,字段置 null)。客人菜单本就刻意不含。
INSERT INTO permissions (permission_code, permission_name, domain, risk_level, requires_approval, status)
VALUES ('bar.internal_settlement_price.view', 'View bar internal settlement (cost) price', 'bar', 'high', FALSE, 'active')
ON CONFLICT (permission_code) DO NOTHING;

-- 平台管理员直授(V206 范式)。其它角色按需另行授权。
INSERT INTO role_permissions (role_id, permission_code)
SELECT 'role_platform_admin', 'bar.internal_settlement_price.view'
ON CONFLICT (role_id, permission_code) DO NOTHING;
