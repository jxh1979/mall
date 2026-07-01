-- 3.5 客人状态防脏值（用户拍板：仅 active）：CHECK 锁死为全系统当前唯一实际写入值，
-- 挡住 bug/误操作写入的未知状态。日后新增封禁/冻结/注销流程时随该功能一并放开对应状态值
-- （YAGNI，不提前预留无流程支撑的空占位值）。
ALTER TABLE customers ADD CONSTRAINT chk_customers_status CHECK (status IN ('active'));
