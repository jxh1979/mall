-- V449 CG-05 核销记录状态闭集（ENUM「核销记录状态」：「成功、拒绝（规则不允许）、失败（设备或网络原因）、已冲正、异常，共五态，
-- 不设「预检通过」态……系统不提供人工放行」）：重建 status CHECK，加入 anomaly（技术决定：英文码）；
-- manual_overridden 按 XM-M034-AB-001 裁决保留（历史行），不再由任何核销链写入。
-- 「异常」的进入条件与「异常状态的统一出口」的主管处置、日结锁定联动待 HTML 补句后另开 PR（本迁移只收口闭集）。
ALTER TABLE ticket_verifications DROP CONSTRAINT IF EXISTS chk_ticket_verifications_status;
ALTER TABLE ticket_verifications
    ADD CONSTRAINT chk_ticket_verifications_status
    CHECK (status IN ('success', 'rejected', 'failed', 'reversed', 'anomaly', 'manual_overridden'));
