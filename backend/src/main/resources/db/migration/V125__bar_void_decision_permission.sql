-- V125 P1-2：酒吧退菜退酒（void）审批分离——新增独立审批权限点 bar.order_item.void_decide。
--
-- 背景（审计实证）：BarController 的申请端点（void-item）与审批端点（order-item-voids/{id}/decision）
-- 此前共用同一权限码 bar.order_item.void（V091 seed），导致持申请权限者可自审批/自驳回，
-- 违背 V117 引入的“先生效后审批”状态机所隐含的审批分离意图（申请人 != 审批人）。
--
-- 本迁移：新增高危需审批权限点 bar.order_item.void_decide，专用于审批决策端点；
-- 申请端点仍用 bar.order_item.void。二者分离后，审批人须单独被授予 void_decide 才能决策，
-- 仅持申请权限者调用决策端点将被 PERMISSION_DENIED 拦截。
--
-- 向后兼容：仅新增权限点与角色授权，不改动既有 bar.order_item.void 语义；历史数据与申请路径不受影响。
-- seed 写法对齐 V091__bar_pos_depth.sql 的 permissions / role_permissions 约定。

-- 1) 新增权限点（高危需审批，与申请权限同 domain/risk 口径）。
INSERT INTO permissions (permission_code, permission_name, domain, risk_level, requires_approval, status)
VALUES
    ('bar.order_item.void_decide',
     'Decide (approve/reject) a bar order item void request (high-risk, segregated from the void requester)',
     'bar', 'high', TRUE, 'active')
ON CONFLICT (permission_code) DO UPDATE SET
    permission_name = EXCLUDED.permission_name,
    domain = EXCLUDED.domain,
    risk_level = EXCLUDED.risk_level,
    requires_approval = EXCLUDED.requires_approval,
    status = EXCLUDED.status;

-- 2) 角色授权：授予 platform_admin（与 V091 既有 bar 高危权限授权口径一致；
--    其余角色按审批分离原则由运营单独授予，避免与申请权限落到同一角色而退化为自审批）。
INSERT INTO role_permissions (role_id, permission_code)
SELECT 'role_platform_admin', permission_code
  FROM permissions
 WHERE permission_code IN (
        'bar.order_item.void_decide'
 )
ON CONFLICT (role_id, permission_code) DO NOTHING;
