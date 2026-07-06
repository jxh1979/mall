-- V258 图形化点单 POS 阶段1 权限点（分类/套餐管理 + 图片上传）：
-- 背景：V256/V257 新增分类(bar_product_categories)、套餐(bar_combos/bar_combo_items)，套餐含内部结算价（敏感成本数据，
--   语义同 V152/V154），故沿用 bar.product.* 的分级授权范式：查看 medium、管理 high。
--   ① bar.category.view / bar.category.manage：分类目录查看/增删改。
--   ② bar.combo.view / bar.combo.manage：套餐目录查看/增删改（含内部结算价，high）。
--   图片上传复用 bar.product.manage（图片是给商品/套餐配图的写操作，属商品管理范畴，不另设权限）。
INSERT INTO permissions (permission_code, permission_name, domain, risk_level, requires_approval, status)
VALUES
    ('bar.category.view', '查看酒吧商品分类目录', 'bar', 'medium', FALSE, 'active'),
    ('bar.category.manage', '维护酒吧商品分类目录（增删改分类、排序、停用）', 'bar', 'medium', FALSE, 'active'),
    ('bar.combo.view', '查看酒吧套餐目录（含内部结算价）', 'bar', 'medium', FALSE, 'active'),
    ('bar.combo.manage', '维护酒吧套餐目录（组合 SKU、售价与内部结算价，属敏感成本数据）', 'bar', 'high', FALSE, 'active')
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
        'bar.category.view',
        'bar.category.manage',
        'bar.combo.view',
        'bar.combo.manage'
 )
ON CONFLICT (role_id, permission_code) DO NOTHING;
