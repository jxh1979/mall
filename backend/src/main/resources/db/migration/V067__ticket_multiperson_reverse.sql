-- R5 T5 多人票核销 + 核销冲正（裁决②：多人票核销 P0、核销冲正 reverse P0）。
-- issued_entitlements：发放时记录核销人数 num_people（多人/联合票>1，普通票默认 1；>1 来源待票种配置，本期默认 1）。
-- ticket_verifications：本次核销人数 verified_people + 冲正关联三件套（reverses_verification_id/reversed_by/reversed_at）。
-- status：现为自由 VARCHAR(32)，本期补 CHECK 收口，真实全集 = success/failed/rejected/reversed/manual_overridden。
--   注意：manual_overridden 由 TicketVerificationService.manualOverride 实际写入，failed/rejected 为权威状态词表保留，
--   漏 manual_overridden 会破坏现有人工放行流。

ALTER TABLE issued_entitlements
    ADD COLUMN IF NOT EXISTS num_people INTEGER NOT NULL DEFAULT 1;

ALTER TABLE ticket_verifications
    ADD COLUMN IF NOT EXISTS verified_people INTEGER NOT NULL DEFAULT 1,
    ADD COLUMN IF NOT EXISTS reverses_verification_id VARCHAR(64) REFERENCES ticket_verifications(id),
    ADD COLUMN IF NOT EXISTS reversed_by VARCHAR(64),
    ADD COLUMN IF NOT EXISTS reversed_at TIMESTAMPTZ;

-- status CHECK 收口（幂等：Flyway 单次执行；列约束首次添加）。真实全集含 manual_overridden。
DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM pg_constraint WHERE conname = 'chk_ticket_verifications_status'
    ) THEN
        ALTER TABLE ticket_verifications
            ADD CONSTRAINT chk_ticket_verifications_status
            CHECK (status IN ('success', 'failed', 'rejected', 'reversed', 'manual_overridden'));
    END IF;
END $$;

-- 核销冲正高风险权限种子（参照 V009 列结构：permission_code/permission_name/domain/risk_level/requires_approval/status）。
INSERT INTO permissions (permission_code, permission_name, domain, risk_level, requires_approval, status)
VALUES
    ('ticket.verify.reverse', 'Reverse ticket verification', 'ticketing', 'high', FALSE, 'active')
ON CONFLICT (permission_code) DO NOTHING;
