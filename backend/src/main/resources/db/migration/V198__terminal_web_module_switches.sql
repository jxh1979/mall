-- 3.19 二期：Web 后台 13 个菜单模块的模块级开关行（terminal_type=web，scope=module，默认开）。
-- 有了显式行，运营即可在矩阵页（ADM-TER-001）逐模块开关；关闭后：
--   前端按 effective 端点隐藏对应菜单组；后端拦截器按 X-Module-Code 逐请求硬拒（TERMINAL_DISABLED）。
-- scope_id = 前端菜单组 key（apps/web/src/admin/adminRoutes.ts group 字段），两侧以此对齐。
INSERT INTO terminal_capability_switches (id, terminal_type, capability_code, scope_type, scope_id, enabled, reason, created_at, updated_at)
VALUES
    ('tcs_web_mod_overview',   'web', 'terminal.enabled', 'module', 'overview',   TRUE, 'Web 后台·总览模块', now(), now()),
    ('tcs_web_mod_access',     'web', 'terminal.enabled', 'module', 'access',     TRUE, 'Web 后台·权限与账号模块', now(), now()),
    ('tcs_web_mod_governance', 'web', 'terminal.enabled', 'module', 'governance', TRUE, 'Web 后台·治理模块', now(), now()),
    ('tcs_web_mod_ticketing',  'web', 'terminal.enabled', 'module', 'ticketing',  TRUE, 'Web 后台·票务模块', now(), now()),
    ('tcs_web_mod_bar',        'web', 'terminal.enabled', 'module', 'bar',        TRUE, 'Web 后台·酒吧模块', now(), now()),
    ('tcs_web_mod_amusement',  'web', 'terminal.enabled', 'module', 'amusement',  TRUE, 'Web 后台·游乐模块', now(), now()),
    ('tcs_web_mod_inventory',  'web', 'terminal.enabled', 'module', 'inventory',  TRUE, 'Web 后台·库存模块', now(), now()),
    ('tcs_web_mod_finance',    'web', 'terminal.enabled', 'module', 'finance',    TRUE, 'Web 后台·财务模块', now(), now()),
    ('tcs_web_mod_risk',       'web', 'terminal.enabled', 'module', 'risk',       TRUE, 'Web 后台·风控模块', now(), now()),
    ('tcs_web_mod_audit',      'web', 'terminal.enabled', 'module', 'audit',      TRUE, 'Web 后台·审计模块', now(), now()),
    ('tcs_web_mod_ops',        'web', 'terminal.enabled', 'module', 'ops',        TRUE, 'Web 后台·运维模块', now(), now()),
    ('tcs_web_mod_vendor',     'web', 'terminal.enabled', 'module', 'vendor',     TRUE, 'Web 后台·外包工作台模块', now(), now()),
    ('tcs_web_mod_integration','web', 'terminal.enabled', 'module', 'integration',TRUE, 'Web 后台·集成与终端模块', now(), now())
ON CONFLICT (id) DO NOTHING;
