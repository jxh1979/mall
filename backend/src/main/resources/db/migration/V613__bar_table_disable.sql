-- 文件版本：V0.01
-- V613：第十八批 AA 段第 206 条 206-R1（台位停用与恢复）。
-- 依据：ENUM:100「桌台/卡座/吧台位状态｜空闲、已预留、已开台、待清台、停用、异常。……台位停用与恢复由店长及以上操作，填原因留痕」；BSM:1058 同句。
-- 停用落 bar_tables.status = 'disabled'（该列本无 CHECK，不另加）；原因与前后状态记在审计留痕里，不另建列。

-- 1) 停用与恢复台位权限（范式同 V579；由运营分配给店长及以上角色）。
INSERT INTO permissions (permission_code, permission_name, domain, risk_level, requires_approval, status)
VALUES ('bar.table.disable', '停用与恢复酒吧台位（店长）', 'bar', 'high', FALSE, 'active')
ON CONFLICT (permission_code) DO NOTHING;
INSERT INTO role_permissions (role_id, permission_code)
VALUES ('role_platform_admin', 'bar.table.disable')
ON CONFLICT (role_id, permission_code) DO NOTHING;

-- 2) 停用、恢复入口 @GuardedAction(capability = bar.table.disable) 的跨端全局显式开启行（照 V611；平台治理地图第 742 行缺行即关）。
INSERT INTO terminal_capability_switches
    (id, terminal_type, capability_code, scope_type, scope_id, enabled, reason, created_at, updated_at)
VALUES
    ('tcs_optin_bar.table.disable', '*', 'bar.table.disable', 'global', 'global', TRUE,
     '已发布在用能力显式开启（平台治理地图第 742 行：缺行默认关闭，须显式打开）', now(), now())
ON CONFLICT (terminal_type, capability_code, scope_type, scope_id) DO NOTHING;
