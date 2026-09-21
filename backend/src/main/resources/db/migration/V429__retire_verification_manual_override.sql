-- V429：M034 审计（2026-09-21）001。
-- 枚举速查「核销记录状态」：「系统不提供人工放行：核销要么成功、要么拒绝或失败，任何岗位、任何权限都不能以人工方式让一笔不满足规则的核销通过」；
-- 权限矩阵文档自 2026-08-06 R10 口径修订起已把 verification.manual_override 标为「停用，待代码轮下线」，字典里却一直是 active
-- （V270 种入并授给平台管理员）。历史迁移不回改，向前修：该码停用，并清掉它的全部授予来源。
--   权限判定先看字典里该码是否 active，不是即一律拒绝（含最高权限）；而有效权限的汇集不联字典状态，所以四处授予都要清。
--   端点与服务方法的下线属票务模块，另行登记。

UPDATE permissions SET status = 'retired' WHERE permission_code = 'verification.manual_override';

DELETE FROM role_permissions WHERE permission_code = 'verification.manual_override';
DELETE FROM permission_template_items WHERE permission_code = 'verification.manual_override';
DELETE FROM user_permission_overrides WHERE permission_code = 'verification.manual_override';

UPDATE temporary_permission_grants
   SET status = 'revoked', revoked_by = 'V429', revoked_at = now()
 WHERE permission_code = 'verification.manual_override'
   AND status = 'active';
