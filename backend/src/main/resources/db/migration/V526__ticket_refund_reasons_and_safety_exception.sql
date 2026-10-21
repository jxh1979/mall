-- 文件版本：V0.01
-- V526 TK-05 普通退款五类闭集与通票安全例外（BSM:804「退款」、BSM:785「退款原则」、BSM:806「退款授权与留痕」；
-- ENUM:47「售票与退款原则」、ENUM:46「有效期类型」、ENUM:148「全园故障与补偿通票」；DEC-R7-20、DEC-R8-94-01、
-- 台账 §33 Q30B1/Q30B2；CG #42；2026-10-21 操作员裁决补充第 5 条「多人通票安全例外只退出事一人份」）。
-- 1) 退款单记录退款理由（五种情形 + 最高权限逐笔放行 + 通票安全例外）、责任主体与适用规则（申请时票种是否允许退款的快照）。
--    旧行不回填，新单由服务端必填。
ALTER TABLE ticket_refunds
    ADD COLUMN IF NOT EXISTS reason_code VARCHAR(32),
    ADD COLUMN IF NOT EXISTS responsible_entity_id VARCHAR(64) REFERENCES business_entities (id),
    ADD COLUMN IF NOT EXISTS ticket_type_refundable_snapshot BOOLEAN;
ALTER TABLE ticket_refunds ADD CONSTRAINT chk_ticket_refunds_reason_code CHECK (reason_code IS NULL OR reason_code IN (
    'ticket_type_rule', 'event_cancelled', 'service_unavailable', 'duplicate_charge', 'legal_requirement',
    'master_override', 'safety_exception'));

-- 2) 通票安全事件：一行＝原票下一个人份（或多次票本次入场的次份）的一次安全事件申请与批准。
--    outcome：refund＝非多次通票免除该人份票款（已核销也退）；restore＝多次通票只恢复本次入场已消耗的 1 次作为完整补偿使用机会。
CREATE TABLE IF NOT EXISTS ticket_safety_events (
    id VARCHAR(64) PRIMARY KEY,
    project_id VARCHAR(64),
    order_id VARCHAR(64) NOT NULL REFERENCES ticket_orders (id),
    parent_ticket_id VARCHAR(64) NOT NULL REFERENCES ticket_instances (id),
    slot_index INTEGER NOT NULL,
    entitlement_id VARCHAR(64) REFERENCES issued_entitlements (id),           -- 本次入场所用的入场类权益
    entry_verification_id VARCHAR(64) REFERENCES ticket_verifications (id),   -- 本次入场核销（未入场为空）
    session_id VARCHAR(64),                                                   -- 原场次（游乐场次，可空）
    reason TEXT NOT NULL,
    situation TEXT NOT NULL,                                                  -- 当时情形
    responsible_entity_id VARCHAR(64) NOT NULL REFERENCES business_entities (id), -- 退款责任主体 / 恢复次数的承担公司
    outcome VARCHAR(16) NOT NULL,
    status VARCHAR(16) NOT NULL,
    requested_by VARCHAR(64) NOT NULL,
    requested_at TIMESTAMPTZ NOT NULL,
    approved_by VARCHAR(64),
    approved_at TIMESTAMPTZ,
    approver_basis VARCHAR(16),                                               -- night_duty | superior | master
    refund_id VARCHAR(64) REFERENCES ticket_refunds (id),
    compensation_batch_id VARCHAR(64) REFERENCES amusement_fault_compensation_batches (id),
    CONSTRAINT chk_ticket_safety_events_outcome CHECK (outcome IN ('refund', 'restore')),
    CONSTRAINT chk_ticket_safety_events_status CHECK (status IN ('pending', 'approved')),
    CONSTRAINT chk_ticket_safety_events_basis CHECK (approver_basis IS NULL OR approver_basis IN ('night_duty', 'superior', 'master'))
);
-- 同一人份只退一次；同一次入场只恢复一次（「同一事件对该次份只恢复一次」）。
CREATE UNIQUE INDEX IF NOT EXISTS uq_ticket_safety_events_refund_share
    ON ticket_safety_events (parent_ticket_id, slot_index) WHERE outcome = 'refund';
CREATE UNIQUE INDEX IF NOT EXISTS uq_ticket_safety_events_restore_entry
    ON ticket_safety_events (entry_verification_id) WHERE outcome = 'restore';

-- 3) 补偿批次/记录可由通票安全事件产生（safety_restore：不关联全园故障与场次结束原件，记录关联安全事件）。
ALTER TABLE amusement_fault_compensation_batches DROP CONSTRAINT IF EXISTS chk_amusement_fault_compensation_batches_event;
ALTER TABLE amusement_fault_compensation_batches ADD CONSTRAINT chk_amusement_fault_compensation_batches_event CHECK (
    (event_type = 'park_wide_fault' AND fault_id IS NOT NULL AND ending_id IS NULL AND session_id IS NULL
        AND initiated_by IS NOT NULL AND initiated_role IS NOT NULL AND initiated_at IS NOT NULL)
 OR (event_type = 'session_early_end' AND fault_id IS NULL AND ending_id IS NOT NULL AND session_id IS NOT NULL)
 OR (event_type = 'safety_restore' AND fault_id IS NULL AND ending_id IS NULL));

ALTER TABLE amusement_fault_compensation_items
    ADD COLUMN IF NOT EXISTS safety_event_id VARCHAR(64) REFERENCES ticket_safety_events (id);
ALTER TABLE amusement_fault_compensation_items DROP CONSTRAINT IF EXISTS chk_amusement_fault_compensation_items_event;
ALTER TABLE amusement_fault_compensation_items ADD CONSTRAINT chk_amusement_fault_compensation_items_event
    CHECK (num_nonnulls(fault_id, ending_id, safety_event_id) = 1);
CREATE UNIQUE INDEX IF NOT EXISTS uq_amusement_fault_compensation_items_safety_once
    ON amusement_fault_compensation_items (safety_event_id) WHERE safety_event_id IS NOT NULL;

DO $$
BEGIN
    IF EXISTS (SELECT 1 FROM pg_roles WHERE rolname = 'complex_app') THEN
        GRANT SELECT, INSERT, UPDATE ON ticket_safety_events TO complex_app;
    END IF;
END $$;
