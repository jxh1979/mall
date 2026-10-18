-- 文件版本：V0.01
-- TK-18 游乐场次提前结束的已入场客人补偿（2026-10-18 操作员裁决补充第 3 条：已入场（已扣次）的客人比照 ENUM「全园故障与补偿通票」
-- 规则补偿，复用 CG-08b 补偿记录与发放确认流程；未入场的票不补偿、可再次使用）。
-- 补偿批次/记录可由「场次提前结束」事件产生：不关联全园故障，关联原场次与结束原件；批次由结束事务系统生成、无发起岗位，
-- 发放前仍须游乐公司负责人与工程或安全岗位先后经同一确认入口确认（第一次岗位确认记为 initiated_*，第二次确认即发放）。
ALTER TABLE amusement_fault_compensation_batches
    ALTER COLUMN fault_id DROP NOT NULL,
    ALTER COLUMN initiated_by DROP NOT NULL,
    ALTER COLUMN initiated_role DROP NOT NULL,
    ALTER COLUMN initiated_at DROP NOT NULL,
    ADD COLUMN event_type VARCHAR(32) NOT NULL DEFAULT 'park_wide_fault',      -- park_wide_fault | session_early_end
    ADD COLUMN ending_id VARCHAR(64) REFERENCES amusement_session_endings (id), -- session_early_end：结束原件
    ADD COLUMN session_id VARCHAR(64) REFERENCES amusement_sessions (id),       -- session_early_end：原场次
    ADD CONSTRAINT chk_amusement_fault_compensation_batches_event CHECK (
        (event_type = 'park_wide_fault' AND fault_id IS NOT NULL AND ending_id IS NULL AND session_id IS NULL
            AND initiated_by IS NOT NULL AND initiated_role IS NOT NULL AND initiated_at IS NOT NULL)
     OR (event_type = 'session_early_end' AND fault_id IS NULL AND ending_id IS NOT NULL AND session_id IS NOT NULL));
-- 一个结束原件只生成一个补偿批次。
CREATE UNIQUE INDEX IF NOT EXISTS uq_amusement_fault_compensation_batches_ending
    ON amusement_fault_compensation_batches (ending_id) WHERE ending_id IS NOT NULL;

ALTER TABLE amusement_fault_compensation_items
    ALTER COLUMN fault_id DROP NOT NULL,
    ADD COLUMN ending_id VARCHAR(64) REFERENCES amusement_session_endings (id),
    ADD CONSTRAINT chk_amusement_fault_compensation_items_event CHECK ((fault_id IS NOT NULL) <> (ending_id IS NOT NULL));
-- 同一次提前结束下同一原人份只补偿一次。
CREATE UNIQUE INDEX IF NOT EXISTS uq_amusement_fault_compensation_items_ending_once
    ON amusement_fault_compensation_items (ending_id, entitlement_id) WHERE ending_id IS NOT NULL;
