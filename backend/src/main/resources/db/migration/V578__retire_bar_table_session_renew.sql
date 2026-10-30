-- 文件版本：V0.01
-- V578：第十八批 K 段第 98 条（98-R2）。
-- 依据：业务地图第 1079 行「平台不提供续卡、续时类产品。」；登记册 XM-M725-O 所选选项「续卡按钮和接口违反 HTML，应删除」，
--   由卡座页面与开台会话后端前后端一起删（同批代码已删 POST /api/v1/bar/table-sessions/{session_id}/renew 与 @GuardedAction bar.table_session.renew）。
-- 照 V433：历史迁移（V019、V251、V554）不回改，向前修——该码停用，并清掉它的全部授予来源；
-- 照 V537：前向删除 V554 种的、已无代码读取的能力开关全局显式行。
-- bar_table_session_adjustments 表、renewed_until 与 additional_* 列、V521 类型约束里的 'renew' 保留不动（转台、并台、拆台共用该表）。

UPDATE permissions SET status = 'retired' WHERE permission_code = 'bar.table_session.renew';

DELETE FROM role_permissions WHERE permission_code = 'bar.table_session.renew';
DELETE FROM permission_template_items WHERE permission_code = 'bar.table_session.renew';
DELETE FROM user_permission_overrides WHERE permission_code = 'bar.table_session.renew';

UPDATE temporary_permission_grants
   SET status = 'revoked', revoked_by = 'V578', revoked_at = now()
 WHERE permission_code = 'bar.table_session.renew'
   AND status = 'active';

DELETE FROM terminal_capability_switches WHERE capability_code = 'bar.table_session.renew';
