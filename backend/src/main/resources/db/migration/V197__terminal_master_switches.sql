-- 3.19 终端十端统一开关（业务HTML「终端开关」节，受控十类终端）：每端一条总开关（global scope）。
-- 默认开（存量端在用，关闭范式只适用新高危能力）；关闭后运营在矩阵页可见可控，
-- 「接口拒绝业务动作 + 兜底关闭」的逐请求后端强制为后续专项。
INSERT INTO terminal_capability_switches (id, terminal_type, capability_code, scope_type, scope_id, enabled, reason, created_at, updated_at)
VALUES
    ('tcs_term_app',      'app',      'terminal.enabled', 'global', 'global', TRUE, '客人 App 端总开关', now(), now()),
    ('tcs_term_miniapp',      'miniapp',      'terminal.enabled', 'global', 'global', TRUE, '小程序端总开关', now(), now()),
    ('tcs_term_web',               'web',               'terminal.enabled', 'global', 'global', TRUE, 'Web 前端总开关', now(), now()),
    ('tcs_term_tablet',            'tablet',            'terminal.enabled', 'global', 'global', TRUE, '工作人员平板总开关', now(), now()),
    ('tcs_term_staff_work_phone',  'staff_work_phone',  'terminal.enabled', 'global', 'global', TRUE, '员工工作手机总开关', now(), now()),
    ('tcs_term_staff_private',  'staff_private_phone',  'terminal.enabled', 'global', 'global', TRUE, '员工私人手机总开关', now(), now()),
    ('tcs_term_merchant_portal',   'merchant_portal',   'terminal.enabled', 'global', 'global', TRUE, '商户门户总开关', now(), now()),
    ('tcs_term_merchant_app',      'merchant_app',      'terminal.enabled', 'global', 'global', TRUE, '商户 App 总开关', now(), now()),
    ('tcs_term_kiosk',             'kiosk',             'terminal.enabled', 'global', 'global', TRUE, '触摸屏一体机总开关', now(), now()),
    ('tcs_term_mobile_admin',      'mobile_admin',      'terminal.enabled', 'global', 'global', TRUE, '移动管理端总开关', now(), now())
ON CONFLICT (id) DO NOTHING;
