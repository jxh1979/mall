-- V278: 溯源归属变更历史 append-only 硬化。V277 注释声明 append-only 且只授 SELECT/INSERT,
-- 但 V140 对未来表设默认 GRANT SELECT,INSERT,UPDATE,DELETE 给 complex_app,显式授权不撤销默认——
-- 故运行时角色仍能改/删归属台账。显式收回,保证归属变更只增不改(防调包对账可信)。
REVOKE UPDATE, DELETE ON inventory_trace_ownership_history FROM complex_app;
