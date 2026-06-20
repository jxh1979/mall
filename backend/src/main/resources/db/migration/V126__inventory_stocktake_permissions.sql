-- V126：盘点录入功能补全（V108 写入侧）权限点。
--
-- 背景（审计实证）：V108 已建 stocktake / inventory_variance 两表，且 finance.postInventoryVariance
-- 依赖 inventory_variance.review_status='approved' 才能入账；但此前无任何写入侧端点能建盘点单 / 录差异 /
-- 把差异复核为 approved → 链路断裂。本迁移补齐盘点录入闭环所需权限点（域 inventory）：
--   1) inventory.stocktake.create          —— 建盘点单（status=counting）。
--   2) inventory.stocktake.variance_record —— 录差异行（variance_qty=counted-book，review_status=pending）。
--   3) inventory.stocktake.variance_review —— 差异复核（approved/rejected）；高危需审批（解锁 finance 入账的关键）。
--   4) inventory.stocktake.view            —— 盘点单列表/详情读端点。
--
-- 复核类（variance_review）按 V108/finance 入账闭环口径定为 high + requires_approval=TRUE；其余为 normal。
-- seed 写法对齐 V125__bar_void_decision_permission.sql 的 permissions / role_permissions 约定。
-- 向后兼容：仅新增权限点与角色授权，不改动既有库存/finance 权限语义。

-- 1) 新增权限点（domain=inventory）。
INSERT INTO permissions (permission_code, permission_name, domain, risk_level, requires_approval, status)
VALUES
    ('inventory.stocktake.create',
     'Create an inventory stocktake (counting)',
     'inventory', 'normal', FALSE, 'active'),
    ('inventory.stocktake.variance_record',
     'Record an inventory stocktake variance line (pending review)',
     'inventory', 'normal', FALSE, 'active'),
    ('inventory.stocktake.variance_review',
     'Review (approve/reject) an inventory stocktake variance; unlocks finance posting to AR/AP (high-risk)',
     'inventory', 'high', TRUE, 'active'),
    ('inventory.stocktake.view',
     'View inventory stocktakes and their variance details',
     'inventory', 'normal', FALSE, 'active')
ON CONFLICT (permission_code) DO UPDATE SET
    permission_name = EXCLUDED.permission_name,
    domain = EXCLUDED.domain,
    risk_level = EXCLUDED.risk_level,
    requires_approval = EXCLUDED.requires_approval,
    status = EXCLUDED.status;

-- 2) 角色授权：授予 platform_admin（与 V125 既有口径一致；复核与录入分离由运营按职责单独授予其余角色）。
INSERT INTO role_permissions (role_id, permission_code)
SELECT 'role_platform_admin', permission_code
  FROM permissions
 WHERE permission_code IN (
        'inventory.stocktake.create',
        'inventory.stocktake.variance_record',
        'inventory.stocktake.variance_review',
        'inventory.stocktake.view'
 )
ON CONFLICT (role_id, permission_code) DO NOTHING;
