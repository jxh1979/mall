-- V127：游乐超容处置策略配置功能补全（V104 写入侧）权限点。
--
-- 背景（审计实证）：V104 已建 amusement_over_capacity_policy 表，读路径
-- （AmusementRepository.findOverCapacityPolicy / AmusementService 核销超容判定）已就绪，
-- 但写入侧全缺（无 save/upsert、无 INSERT），所有项目恒退化为 disabled 占位 →
-- 软上限告警与 warn_only 模式「有读取代码但永不触发」。本迁移补齐写入侧 PUT/GET 端点所需
-- 权限点（域 amusement）：
--   1) amusement.over_capacity_policy.configure —— upsert 策略（运营容量软上限 + 处置模式），risk 偏高。
--   2) amusement.over_capacity_policy.view      —— 读当前策略。
--
-- configure 按写入危险度定为 high；view 为 normal。
-- seed 写法对齐 V125/V126 的 permissions / role_permissions 约定。
-- 向后兼容：仅新增权限点与角色授权，不改动既有 amusement 权限语义。

-- 1) 新增权限点（domain=amusement）。
INSERT INTO permissions (permission_code, permission_name, domain, risk_level, requires_approval, status)
VALUES
    ('amusement.over_capacity_policy.configure',
     'Configure an amusement project over-capacity disposal policy (operating soft-cap + disposal mode, high-risk)',
     'amusement', 'high', FALSE, 'active'),
    ('amusement.over_capacity_policy.view',
     'View an amusement project over-capacity disposal policy',
     'amusement', 'normal', FALSE, 'active')
ON CONFLICT (permission_code) DO UPDATE SET
    permission_name = EXCLUDED.permission_name,
    domain = EXCLUDED.domain,
    risk_level = EXCLUDED.risk_level,
    requires_approval = EXCLUDED.requires_approval,
    status = EXCLUDED.status;

-- 2) 角色授权：授予 platform_admin（与 V125/V126 既有口径一致）。
INSERT INTO role_permissions (role_id, permission_code)
SELECT 'role_platform_admin', permission_code
  FROM permissions
 WHERE permission_code IN (
        'amusement.over_capacity_policy.configure',
        'amusement.over_capacity_policy.view'
 )
ON CONFLICT (role_id, permission_code) DO NOTHING;
