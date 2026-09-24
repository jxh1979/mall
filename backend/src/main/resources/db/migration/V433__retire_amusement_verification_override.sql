-- V433：XM-M034-AB（2026-09-24）004，操作员 D-033「删除」。
-- 枚举速查「核销记录状态」：「系统不提供人工放行：核销要么成功、要么拒绝或失败，任何岗位、任何权限都不能以人工方式让一笔不满足规则的核销通过」；
-- 业务地图第 1263 行：「在玩人数不允许人工调整：任何岗位（含主管与最高权限）都不得手工修改、清零或修正在玩人数」。
-- 游乐人工改判（allow/deny/pause/resume/release）入口整体下线，其权限码照 V429 停用并清掉全部授予来源；
-- amusement_manual_overrides 表与历史行、V014/V234 的类型约束保留不动。

UPDATE permissions SET status = 'retired' WHERE permission_code = 'amusement.verification.override';

DELETE FROM role_permissions WHERE permission_code = 'amusement.verification.override';
DELETE FROM permission_template_items WHERE permission_code = 'amusement.verification.override';
DELETE FROM user_permission_overrides WHERE permission_code = 'amusement.verification.override';

UPDATE temporary_permission_grants
   SET status = 'revoked', revoked_by = 'V433', revoked_at = now()
 WHERE permission_code = 'amusement.verification.override'
   AND status = 'active';
