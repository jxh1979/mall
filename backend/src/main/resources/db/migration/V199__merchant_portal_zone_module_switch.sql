-- 3.18 商户专区一期（方案A=后台内商户专区）：merchant_portal 终端的「商户专区」菜单组模块开关行。
-- scope_id = 前端菜单组 key（apps/web/src/admin/adminRoutes.ts group="merchant"），与 V198 web 模块行同范式。
-- merchant 归属账号登录后台后整个会话按 merchant_portal 终端上报：
--   总开关 = V197 tcs_term_merchant_portal（关闭即后端逐请求硬拒 + 前端除自救组外全隐）；
--   本行 = 商户专区菜单组模块开关（关闭即隐藏菜单 + 后端按 X-Module-Code 硬拒）。
INSERT INTO terminal_capability_switches (id, terminal_type, capability_code, scope_type, scope_id, enabled, reason, created_at, updated_at)
VALUES
    ('tcs_mp_mod_merchant', 'merchant_portal', 'terminal.enabled', 'module', 'merchant', TRUE, '商户专区·我的账单/工单/对账/合同/费用通知', now(), now())
ON CONFLICT (id) DO NOTHING;
