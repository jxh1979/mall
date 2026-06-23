-- V154 酒吧商品管理权限（ADM-BAR-005 商品管理页 + /bar/products 接口收紧）：
-- 背景：商品目录含 V152 internal_settlement_price_cent（平台↔酒吧成本结算价基，敏感数据）。此前 GET/POST
--   /api/v1/bar/products 均搭便车 bar.order.create——凡能下单的角色（收银/吧台）都能看改内部结算价，违反
--   敏感成本数据应独立授权的治理原则（业主 2026-06-23 确认采用专用权限）。本迁移新增：
--   ① bar.product.view（medium）查看商品目录（含内部结算价）；② bar.product.manage（high）录入/修改商品（含内部价）。
--   控制器 listProducts 改用 bar.product.view、createProduct 改用 bar.product.manage。
INSERT INTO permissions (permission_code, permission_name, domain, risk_level, requires_approval, status)
VALUES
    ('bar.product.view', 'View bar product catalog including internal settlement price', 'bar', 'medium', FALSE, 'active'),
    ('bar.product.manage', 'Create or update bar products including the sensitive internal settlement price', 'bar', 'high', FALSE, 'active')
ON CONFLICT (permission_code) DO UPDATE SET
    permission_name = EXCLUDED.permission_name,
    domain = EXCLUDED.domain,
    risk_level = EXCLUDED.risk_level,
    requires_approval = EXCLUDED.requires_approval,
    status = EXCLUDED.status;

INSERT INTO role_permissions (role_id, permission_code)
SELECT 'role_platform_admin', permission_code
  FROM permissions
 WHERE permission_code IN (
        'bar.product.view',
        'bar.product.manage'
 )
ON CONFLICT (role_id, permission_code) DO NOTHING;
