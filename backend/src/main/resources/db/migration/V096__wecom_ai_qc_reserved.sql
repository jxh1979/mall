-- V096 企微 AI 语义质检：预留接口（端口/适配器）+ 能力开关（默认关，后台可配）。
-- 用户口径：第三方/AI 一律"预留接口 + 能力开关默认关；开关后台可配；
--   关→走现有关键词质检（不调 AI、不阻塞）；开→路由 AI 适配器（当前无真实厂商=占位 NOT_CONFIGURED，留接缝）"。
-- 背景：R5 T8（V087）已落地基础关键词质检（risk_flags），AI 语义质检（情绪/投诉倾向/语义风险）此前为 P2。
--   本迁移补齐 AI 语义质检接缝：能力开关 + wecom_messages AI 质检结果预留字段，不接真实 AI（待资料/可接本地 LLM）。
-- 事实源对齐：terminal_capability_switches（R5 T2，V068 CHECK 含 terminal_type '*' 与 scope 'global'）、
--   wecom_messages（V087）、permissions/role_permissions（既有 RBAC，复用 wecom.retention.manage 管理 AI 质检开关）。

-- ── 1) 能力开关：wecom.ai_qc.enabled 显式 seed = FALSE（默认关） ──────────────────
-- 注意：T2 TerminalSwitchGuard / resolveEffectiveSwitch 语义为"无显式行=默认开启"（opt-out）。
--   AI 质检要求"默认关"，故必须显式插入 enabled=FALSE 行；terminal_type='*'（跨端通配）、scope_type='global'。
-- 后台经现有 TerminalDeviceController.updateCapabilitySwitch 端点切换本开关（与 MDM/支付渠道/第三方同机制）。
INSERT INTO terminal_capability_switches
    (id, terminal_type, capability_code, scope_type, scope_id, enabled, reason, created_at, updated_at)
VALUES
    ('tcs_wecom_ai_qc_off', '*', 'wecom.ai_qc.enabled', 'global', 'global', FALSE,
     '企微 AI 语义质检默认关闭：关时仅走基础关键词质检（risk_flags，不调 AI、不阻塞）；'
     || '待真实 AI/LLM 质检 SDK 接入（或本地 LLM）+ 后台 updateCapabilitySwitch 开启后路由至 AI 适配器', now(), now())
ON CONFLICT (terminal_type, capability_code, scope_type, scope_id) DO NOTHING;

-- ── 2) wecom_messages AI 质检结果预留字段 ───────────────────────────────────────
-- ai_qc_status：AI 语义质检状态。开关关→保持默认 'skipped'（不调 AI）；开关开→占位适配器写 'not_configured'；
--   真实适配器接入后写 'analyzed'（成功）/'failed'（适配器异常，不阻塞落库与关键词结果）。
-- ai_qc_result：AI 语义质检结构化结果预留（情绪 sentiment / 投诉倾向 complaint_risk / 语义风险标签 ai_risk_flags 等），
--   占位实现为 NULL；真实适配器接入后回填 JSONB。与 risk_flags（关键词命中）独立并存，互不覆盖。
ALTER TABLE wecom_messages
    ADD COLUMN IF NOT EXISTS ai_qc_status VARCHAR(32) NOT NULL DEFAULT 'skipped',
    ADD COLUMN IF NOT EXISTS ai_qc_result JSONB;

-- AI 质检状态枚举约束（skipped=未调 / not_configured=开关开但无厂商 / analyzed=成功 / failed=适配器异常）。
ALTER TABLE wecom_messages
    DROP CONSTRAINT IF EXISTS chk_wecom_msg_ai_qc_status;
ALTER TABLE wecom_messages
    ADD CONSTRAINT chk_wecom_msg_ai_qc_status
    CHECK (ai_qc_status IN ('skipped', 'not_configured', 'analyzed', 'failed'));

-- ── 3) 权限 seed：复用 wecom.retention.manage 管理 AI 质检开关/适配器 ───────────────
-- 用户口径"后台可配"复用既有质检/留存管理权限（wecom.retention.manage 含质检策略管理语义，V087 已 seed）。
-- 为审计语义明确，另显式 seed 可选独立权限 wecom.ai_qc.manage（platform_admin 默认拥有），
--   后台切换 wecom.ai_qc.enabled 时校验该权限或 wecom.retention.manage 均可。
INSERT INTO permissions (permission_code, permission_name, domain, risk_level, requires_approval, status)
VALUES
    ('wecom.ai_qc.manage', 'Manage WeCom AI semantic quality-check capability switch and adapters',
     'compliance', 'high', TRUE, 'active')
ON CONFLICT (permission_code) DO UPDATE SET
    permission_name = EXCLUDED.permission_name,
    domain = EXCLUDED.domain,
    risk_level = EXCLUDED.risk_level,
    requires_approval = EXCLUDED.requires_approval,
    status = EXCLUDED.status;

INSERT INTO role_permissions (role_id, permission_code)
SELECT 'role_platform_admin', permission_code
  FROM permissions
 WHERE permission_code IN ('wecom.ai_qc.manage')
ON CONFLICT (role_id, permission_code) DO NOTHING;
