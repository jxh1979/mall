-- V330 结算配置中心批1(通票分账):过账触发权限点(补 postBill 生产触发缺口)。
-- 缘由:月度结算引擎算出 confirmed 单后,生产中【无任何路径】触发过账(postBill 生产零调用,经对抗复核坐实)
--       → ticket_distribution 应付台账在生产从未被写。本批补两条生产触发:
--   ① 月结作业 SettlementMonthlyJob 对未越池、已 confirmed 的常规单自动过账(仍在既有开关
--      settlement.monthly.scheduled.enabled 后·默认关);
--   ② 网关端点 POST /api/v1/settlement/bills/{id}/post 供超池特批 confirmed 后人工过账(需本高敏权限)。
-- 🔴 落码≠上线:本迁移只加权限,不激活任何自动过账。生产【首次真过账】= 运营激活决定
--   (翻 settlement.monthly.scheduled.enabled 开关 / 人工调端点),另行拍板,不在本迁移、不在部署默认。
-- 本迁移仅加权限 + 授权,不改表结构,可独立回滚。

-- ========== 1) 过账写权限点(高敏 requires_approval,照 settlement.config.manage 范式)==========
INSERT INTO permissions (permission_code, permission_name, domain, risk_level, requires_approval, status)
VALUES
    ('settlement.bill.post', '结算单过账(挂应付台账·高敏)', 'settlement', 'high', TRUE, 'active')
ON CONFLICT (permission_code) DO NOTHING;

-- 授予:仅最高权限(沿用现状高危授权约定,与 settlement.config.manage 一致)。
INSERT INTO role_permissions (role_id, permission_code)
SELECT v.role_id, v.permission_code
FROM (VALUES
    ('role_platform_admin', 'settlement.bill.post')
) AS v(role_id, permission_code)
ON CONFLICT (role_id, permission_code) DO NOTHING;
