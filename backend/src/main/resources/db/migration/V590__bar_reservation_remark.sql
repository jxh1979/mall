-- 文件版本：V0.01
-- V590（第十八批 N 段第 120 条 120-R3）：订台加订台备注。
-- 依据：BSM:987「支持订台下单：选择酒吧、日期、时段、卡座/桌台/吧台位、人数和订台备注；订台需要支付定金。」
-- 只加可空列，不回填存量数据。

ALTER TABLE bar_reservations ADD COLUMN IF NOT EXISTS remark TEXT;
COMMENT ON COLUMN bar_reservations.remark IS '订台备注（下单时填写，BSM:987）';
