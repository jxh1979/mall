-- 文件版本：V0.01
-- 第十八批 F 段第 14 条 14-R1 通用人工补录主链。
-- BSM 第 1642 行「记录缺失时允许人工补录，但必须写原因和责任人。补录时限为业务发生后的配置天数内（安全阈值，按项目可配，调整须最高权限审批并留痕，
-- 出厂默认 3 天，须同时配置上下限），超时不得补录；补录必须附原始记录凭证（微信截图、通话录音等）并经审核，以通话录音为凭证的，录音起始时间必须早于
-- 客人到店时间、且早于客人开卡时间达到后台配置的时长（业务参数，按项目可配，出厂默认 30 分钟，须同时配置上下限），不满足的不予采信。补录一律进入主管
-- 复核队列，复核通过才生效；……补录记录与原始记录并列展示，标明“人工补录”。」
-- 三类补录对象一次建全：交接扫码缺段（trace_handover_step）、台位会话销售归属（bar_session_sales）、台位会话服务责任区间（bar_session_service）。
-- 补录单只记申请与结论，原始记录不改（并列展示、互不覆盖）；批准时由各对象的实现落业务。

-- 1) 补录单
CREATE TABLE IF NOT EXISTS manual_backfills (
    id VARCHAR(64) PRIMARY KEY,
    target_type VARCHAR(32) NOT NULL,
    target_id VARCHAR(128) NOT NULL,
    target_part VARCHAR(40),
    project_id VARCHAR(64),
    merchant_id VARCHAR(64),
    business_at TIMESTAMPTZ NOT NULL,
    occurred_at TIMESTAMPTZ NOT NULL,
    proposed JSONB NOT NULL,
    original_snapshot JSONB NOT NULL,
    reason TEXT NOT NULL,
    responsible_staff_id VARCHAR(64) NOT NULL,
    evidence_attachment_ids JSONB NOT NULL,
    recording_started_at TIMESTAMPTZ,
    review_id VARCHAR(64) NOT NULL UNIQUE,
    status VARCHAR(16) NOT NULL DEFAULT 'pending',
    submitted_by VARCHAR(64) NOT NULL,
    submitted_at TIMESTAMPTZ NOT NULL,
    decided_at TIMESTAMPTZ,
    applied_at TIMESTAMPTZ,
    CONSTRAINT chk_manual_backfills_target_type
        CHECK (target_type IN ('trace_handover_step', 'bar_session_sales', 'bar_session_service')),
    CONSTRAINT chk_manual_backfills_status CHECK (status IN ('pending', 'approved', 'rejected')),
    CONSTRAINT chk_manual_backfills_evidence CHECK (jsonb_array_length(evidence_attachment_ids) > 0)
);
CREATE INDEX IF NOT EXISTS idx_manual_backfills_target ON manual_backfills (target_type, target_id);

COMMENT ON TABLE manual_backfills IS '人工补录单（BSM 第 1642 行）：一律进主管复核，复核通过才生效；原始记录不改，补录与原始记录并列展示';
COMMENT ON COLUMN manual_backfills.target_part IS '补录的部位（交接扫码补录为交接段名，其余为空）';
COMMENT ON COLUMN manual_backfills.project_id IS '补录对象所属项目（取补录参数用；平台直营交接无商户无项目时为空，按出厂参数）';
COMMENT ON COLUMN manual_backfills.business_at IS '业务发生时刻（补录时限自此起算）';
COMMENT ON COLUMN manual_backfills.occurred_at IS '被补录事实的原时点';
COMMENT ON COLUMN manual_backfills.proposed IS '拟补录内容';
COMMENT ON COLUMN manual_backfills.original_snapshot IS '提交时的原始记录（并列展示用）';
COMMENT ON COLUMN manual_backfills.responsible_staff_id IS '责任人';
COMMENT ON COLUMN manual_backfills.evidence_attachment_ids IS '原始记录凭证附件编号（至少一个）';
COMMENT ON COLUMN manual_backfills.recording_started_at IS '以通话录音为凭证时的录音起始时间（提交人填写，复核人对照文件核实）';
COMMENT ON COLUMN manual_backfills.review_id IS '主管复核单编号';
COMMENT ON COLUMN manual_backfills.applied_at IS '批准后落入业务的时刻';

-- 2) 补录参数（按项目；项目无自有行即用 '__global__' 出厂行：时限 3 天、录音提前 30 分钟，上下限留空，首次配置时与值同填）
CREATE TABLE IF NOT EXISTS manual_backfill_policies (
    project_id VARCHAR(64) PRIMARY KEY,
    window_days INTEGER NOT NULL,
    window_min_days INTEGER,
    window_max_days INTEGER,
    recording_lead_minutes INTEGER NOT NULL,
    lead_min_minutes INTEGER,
    lead_max_minutes INTEGER,
    updated_by VARCHAR(128),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT chk_manual_backfill_policies_window CHECK (
        (window_min_days IS NULL AND window_max_days IS NULL AND window_days >= 1)
        OR (window_min_days >= 1 AND window_min_days <= window_days AND window_days <= window_max_days)),
    CONSTRAINT chk_manual_backfill_policies_lead CHECK (
        (lead_min_minutes IS NULL AND lead_max_minutes IS NULL AND recording_lead_minutes >= 0)
        OR (lead_min_minutes >= 0 AND lead_min_minutes <= recording_lead_minutes AND recording_lead_minutes <= lead_max_minutes))
);
INSERT INTO manual_backfill_policies (project_id, window_days, window_min_days, window_max_days,
    recording_lead_minutes, lead_min_minutes, lead_max_minutes)
VALUES ('__global__', 3, NULL, NULL, 30, NULL, NULL)
ON CONFLICT (project_id) DO NOTHING;
COMMENT ON TABLE manual_backfill_policies IS '人工补录参数（BSM 第 1642 行）：补录时限为安全阈值（调整须最高权限本人办理），录音提前量为业务参数；按项目，值与上下限同配';
COMMENT ON COLUMN manual_backfill_policies.window_days IS '补录时限（业务发生后天数）';
COMMENT ON COLUMN manual_backfill_policies.recording_lead_minutes IS '通话录音起始须早于到店与开卡的分钟数';

-- 3) 权限码（范式同 V548、V559）：配置补录参数；店长复核销售归属补录（由运营分配给店长角色）。
INSERT INTO permissions (permission_code, permission_name, domain, risk_level, requires_approval, status)
VALUES ('evidence.manual_backfill.policy.manage', '配置人工补录参数（按项目）', 'evidence', 'high', TRUE, 'active'),
       ('bar.sales_backfill.review', '店长复核销售归属补录', 'bar', 'high', FALSE, 'active')
ON CONFLICT (permission_code) DO NOTHING;
INSERT INTO role_permissions (role_id, permission_code)
VALUES ('role_platform_admin', 'evidence.manual_backfill.policy.manage'),
       ('role_platform_admin', 'bar.sales_backfill.review')
ON CONFLICT (role_id, permission_code) DO NOTHING;

DO $$
BEGIN
    IF EXISTS (SELECT 1 FROM pg_roles WHERE rolname = 'complex_app') THEN
        EXECUTE 'GRANT SELECT, INSERT, UPDATE ON manual_backfills TO complex_app';
        EXECUTE 'GRANT SELECT, INSERT, UPDATE ON manual_backfill_policies TO complex_app';
    END IF;
END $$;
