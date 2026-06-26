-- V171 修合同退租结算 status 列宽与默认值漏洞：
-- V163 把结算状态 CHECK 从3态改为5态(pending_settlement/deposit_offsetting/arrears_collecting/settled/disputed)，
-- 但 V162 的 status 列是 VARCHAR(16) 且默认 'in_progress'——5态最长18字符超 16、默认值违反新 CHECK。
-- 单测走内存仓未覆盖列宽，真结算插入(集成测试)才暴露。扩列宽 + 改默认为合法态。
ALTER TABLE contract_settlements ALTER COLUMN status TYPE VARCHAR(32);
ALTER TABLE contract_settlements ALTER COLUMN status SET DEFAULT 'pending_settlement';
