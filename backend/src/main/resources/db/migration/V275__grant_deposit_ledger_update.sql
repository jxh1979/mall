-- V275: 押金动作 500(permission denied for deposit_ledger)修复。
-- 真实用户流程审计 2026-07-08 发现:押金收取/退还走 SELECT ... FOR UPDATE 锁定押金余额行,
-- 需 UPDATE 权限,但受限运行时角色 complex_app 在 deposit_ledger 上仅有 INSERT/SELECT(迁移遗漏授权)
-- —— 与 V273 修复的 ar_ap_payments 同一类系统性隐患(事务表漏授 UPDATE)。

GRANT UPDATE, DELETE ON deposit_ledger TO complex_app;
