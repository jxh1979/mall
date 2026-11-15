-- 文件版本：V0.01
-- V626：第十八批 AK 段第 230 条 230-R1（人工取消订台限店长及以上）。
-- 依据：BSM:1058「已预留只能经取消订台、超时释放、到店开台三条路径退出；店长及以上可人工取消预留，必须填原因并留痕。」；ENUM:100 同句。
-- 原因与操作人、时间记在审计留痕里，不另建列（同 V613）。

-- 1) 人工取消订台权限（范式同 V613；由运营分配给店长及以上角色）。
INSERT INTO permissions (permission_code, permission_name, domain, risk_level, requires_approval, status)
VALUES ('bar.reservation.cancel', '人工取消订台（店长）', 'bar', 'high', FALSE, 'active')
ON CONFLICT (permission_code) DO NOTHING;
INSERT INTO role_permissions (role_id, permission_code)
VALUES ('role_platform_admin', 'bar.reservation.cancel')
ON CONFLICT (role_id, permission_code) DO NOTHING;

-- 2) 取消入口 @GuardedAction(capability = bar.reservation.cancel) 的跨端全局显式开启行（照 V613；平台治理地图第 742 行缺行即关）。
INSERT INTO terminal_capability_switches
    (id, terminal_type, capability_code, scope_type, scope_id, enabled, reason, created_at, updated_at)
VALUES
    ('tcs_optin_bar.reservation.cancel', '*', 'bar.reservation.cancel', 'global', 'global', TRUE,
     '已发布在用能力显式开启（平台治理地图第 742 行：缺行默认关闭，须显式打开）', now(), now())
ON CONFLICT (terminal_type, capability_code, scope_type, scope_id) DO NOTHING;
