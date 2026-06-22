-- 防重 DB 兜底唯一约束（此前仅靠应用层行锁+查重，缺 DB 层兜底；本迁移补 UNIQUE，绕过应用路径的直写也无法重复）。
-- 背景：审计指出 bar_commission_calculations.bill_id / cam_trueup_runs.settlement_period 仅普通 INDEX 非 UNIQUE。
-- 注意：若历史数据已存在重复，本迁移会失败，须先按业务清理重复行（保留权威一笔）后再迁移——这是有意暴露数据问题，不静默放过。

-- 提成：一张账单至多一笔提成计提（calculateCommission 查重已保证，加 UNIQUE 兜底防绕过路径双计提）。
CREATE UNIQUE INDEX IF NOT EXISTS uq_bar_commission_calculations_bill
    ON bar_commission_calculations (bill_id);

-- CAM true-up：同一 settlement_period 至多一笔 completed true-up（void 不限，允许作废后重做）。
-- 部分唯一索引：仅约束 status='completed'，防"同期重复 true-up 致差额翻倍入账"。
CREATE UNIQUE INDEX IF NOT EXISTS uq_cam_trueup_runs_period_completed
    ON cam_trueup_runs (settlement_period)
    WHERE status = 'completed';
