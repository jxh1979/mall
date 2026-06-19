-- R5 T17 扫码交接全链状态机（供应链×留痕域）。
-- 裁决依据：二轮裁决——员工扫码主动留痕 + 机器人上报被动留痕；缺口/重复/差异进复核。
-- 依据：docs/r5-spec-T17-trace-handover-chain-dev-design-20260618.md；现状取自 V012（inventory_trace_handovers 单条平面记录）。
-- 口径：
--   1) 保留 inventory_trace_handovers 既有列（含 handover_result）不动，补整体状态 overall_status/current_segment（既有行默认 in_progress，兼容）。
--   2) 新建 inventory_trace_handover_steps（五段链）+ inventory_trace_handover_exceptions（gap/duplicate/diff → 复核）。
--   3) seed 权限 inventory.handover.scan / inventory.handover.review。
--   4) 机器人段（dispatch）被动留痕受 T14 integration.robot.sync 开关门控，可关联 V071 integration_robot_task_events(handover_id)。

-- ── 1) 表头补整体状态（既有行得默认值，兼容） ─────────────────────────────────
ALTER TABLE inventory_trace_handovers
    ADD COLUMN IF NOT EXISTS overall_status VARCHAR(32) NOT NULL DEFAULT 'in_progress',
    ADD COLUMN IF NOT EXISTS current_segment VARCHAR(40);

-- chk_trace_handover_overall：仅当不存在时添加（幂等）。
DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM pg_constraint WHERE conname = 'chk_trace_handover_overall'
    ) THEN
        ALTER TABLE inventory_trace_handovers
            ADD CONSTRAINT chk_trace_handover_overall CHECK
                (overall_status IN ('in_progress', 'completed', 'exception'));
    END IF;
END $$;

-- ── 2) 分段步骤（五段链） ───────────────────────────────────────────────────
CREATE TABLE IF NOT EXISTS inventory_trace_handover_steps (
    id VARCHAR(64) PRIMARY KEY,
    handover_id VARCHAR(64) NOT NULL REFERENCES inventory_trace_handovers(id) ON DELETE CASCADE,
    segment VARCHAR(40) NOT NULL,            -- 五段
    seq SMALLINT NOT NULL,                   -- 段序 1..5
    actor_type VARCHAR(16) NOT NULL,         -- staff(主动扫码) / robot(被动上报)
    operator_user_id VARCHAR(64),            -- staff 段
    robot_task_ref VARCHAR(128),             -- robot 段
    scan_at TIMESTAMPTZ,
    step_status VARCHAR(24) NOT NULL DEFAULT 'pending', -- pending/done/gap/duplicate/diff
    note VARCHAR(255),
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT chk_trace_step_segment CHECK (segment IN
        ('out_verify', 'dispatch', 'kitchen_receive', 'staff_pickup', 'deliver_customer')),
    CONSTRAINT chk_trace_step_actor CHECK (actor_type IN ('staff', 'robot')),
    CONSTRAINT chk_trace_step_status CHECK (step_status IN ('pending', 'done', 'gap', 'duplicate', 'diff')),
    CONSTRAINT uq_trace_step UNIQUE (handover_id, segment)
);

CREATE INDEX IF NOT EXISTS idx_inventory_trace_handover_steps_handover
    ON inventory_trace_handover_steps (handover_id, seq);

-- ── 3) 交接异常 → 复核（缺口/重复/差异） ─────────────────────────────────────
CREATE TABLE IF NOT EXISTS inventory_trace_handover_exceptions (
    id VARCHAR(64) PRIMARY KEY,
    handover_id VARCHAR(64) NOT NULL REFERENCES inventory_trace_handovers(id),
    segment VARCHAR(40) NOT NULL,
    exception_type VARCHAR(16) NOT NULL,     -- gap/duplicate/diff
    review_status VARCHAR(32) NOT NULL DEFAULT 'pending',
    reviewer_user_id VARCHAR(64),
    reason VARCHAR(255),
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    reviewed_at TIMESTAMPTZ,
    CONSTRAINT chk_trace_exc_type CHECK (exception_type IN ('gap', 'duplicate', 'diff')),
    CONSTRAINT chk_trace_exc_status CHECK (review_status IN ('pending', 'approved', 'rejected'))
);

CREATE INDEX IF NOT EXISTS idx_inventory_trace_handover_exceptions_handover
    ON inventory_trace_handover_exceptions (handover_id, created_at DESC);

CREATE INDEX IF NOT EXISTS idx_inventory_trace_handover_exceptions_review
    ON inventory_trace_handover_exceptions (review_status, created_at DESC);

-- ── 4) 权限点 seed（inventory.handover.scan 扫码/上报；inventory.handover.review 异常复核） ──
INSERT INTO permissions (permission_code, permission_name, domain, risk_level, requires_approval, status)
VALUES
    ('inventory.handover.scan',   'Scan/report inventory trace handover segments', 'inventory', 'high', TRUE, 'active'),
    ('inventory.handover.review', 'Review inventory trace handover exceptions',     'inventory', 'high', TRUE, 'active')
ON CONFLICT (permission_code) DO UPDATE SET
    permission_name = EXCLUDED.permission_name,
    domain = EXCLUDED.domain,
    risk_level = EXCLUDED.risk_level,
    requires_approval = EXCLUDED.requires_approval,
    status = EXCLUDED.status;

INSERT INTO role_permissions (role_id, permission_code)
SELECT 'role_platform_admin', permission_code
  FROM permissions
 WHERE permission_code IN ('inventory.handover.scan', 'inventory.handover.review')
ON CONFLICT (role_id, permission_code) DO NOTHING;
