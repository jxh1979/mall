-- R5 T8 企微会话合规链（PIPL 平台侧合规能力）。
-- 平台侧本期 P0：可配置留存（默认 180 天）+ 到期归档/销毁/匿名化 + 普通展示脱敏 + 原文调阅最小授权+审批+审计。
-- 企微真实同步按第三方原则预留：同步入站受 T2 终端能力开关 integration.wecom.sync 门控（V071 已 seed enabled=false）。
-- 现状基准：企微仅 V015 一行 mock 种子，无留存表/上传接口/到期触发/脱敏链/查看审计。本迁移补齐四表。

-- 留存策略（可配置，默认 180 天）。
CREATE TABLE IF NOT EXISTS wecom_retention_policies (
    id VARCHAR(64) PRIMARY KEY,
    scope_type VARCHAR(32) NOT NULL DEFAULT 'global',        -- global/project/merchant
    scope_id VARCHAR(64) NOT NULL DEFAULT '*',
    retention_days INTEGER NOT NULL DEFAULT 180,
    expiry_action VARCHAR(16) NOT NULL DEFAULT 'anonymize',  -- archive/delete/anonymize
    enabled BOOLEAN NOT NULL DEFAULT true,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT chk_wecom_ret_scope_type CHECK (scope_type IN ('global', 'project', 'merchant')),
    CONSTRAINT chk_wecom_ret_action CHECK (expiry_action IN ('archive', 'delete', 'anonymize')),
    CONSTRAINT chk_wecom_ret_days CHECK (retention_days BETWEEN 1 AND 3650),
    CONSTRAINT uq_wecom_ret_scope UNIQUE (scope_type, scope_id)
);

-- 会话归属（员工/客户/外部会话 id/项目）。
CREATE TABLE IF NOT EXISTS wecom_conversations (
    id VARCHAR(64) PRIMARY KEY,
    external_conversation_id VARCHAR(128) NOT NULL UNIQUE,
    project_id VARCHAR(64) NOT NULL,
    owner_staff_id VARCHAR(64),
    customer_ref VARCHAR(128),
    archive_status VARCHAR(32) NOT NULL DEFAULT 'active',    -- active/archived/anonymized/deleted
    retention_until DATE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT chk_wecom_conv_status CHECK (archive_status IN ('active', 'archived', 'anonymized', 'deleted'))
);

CREATE INDEX IF NOT EXISTS idx_wecom_conversations_retention
    ON wecom_conversations (archive_status, retention_until);

-- 消息（发送方/脱敏后展示内容/原文密文或引用/敏感命中质检/business_date）。
CREATE TABLE IF NOT EXISTS wecom_messages (
    id VARCHAR(64) PRIMARY KEY,
    conversation_id VARCHAR(64) NOT NULL REFERENCES wecom_conversations(id) ON DELETE CASCADE,
    sender_ref VARCHAR(128),
    content_raw_ref TEXT,                                    -- 原文引用/密文（仅授权可解）
    content_redacted TEXT,                                   -- 脱敏文本（普通展示用）
    risk_flags JSONB NOT NULL DEFAULT '[]',                  -- 质检命中规则码集合
    anonymized BOOLEAN NOT NULL DEFAULT false,
    sent_at TIMESTAMPTZ,
    business_date DATE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX IF NOT EXISTS idx_wecom_messages_conversation
    ON wecom_messages (conversation_id, sent_at);

-- 原文调阅审计（看原文必写：viewer/approval_ref/viewed_at/字段）。
CREATE TABLE IF NOT EXISTS wecom_access_audits (
    id VARCHAR(64) PRIMARY KEY,
    conversation_id VARCHAR(64) NOT NULL,
    viewer_user_id VARCHAR(64) NOT NULL,
    view_type VARCHAR(16) NOT NULL,                         -- redacted/raw
    approval_ref VARCHAR(64),                               -- raw 必须带审批引用
    viewed_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT chk_wecom_access_type CHECK (view_type IN ('redacted', 'raw'))
);

CREATE INDEX IF NOT EXISTS idx_wecom_access_audits_conversation
    ON wecom_access_audits (conversation_id, viewed_at);

-- 默认留存策略：全局 180 天匿名化。
INSERT INTO wecom_retention_policies (id, scope_type, scope_id, retention_days, expiry_action, enabled)
VALUES ('wrp_global_default', 'global', '*', 180, 'anonymize', true)
ON CONFLICT (scope_type, scope_id) DO NOTHING;

-- 权限：脱敏展示（wecom.view）/原文调阅（wecom.view_raw，高敏需审批）/留存与质检管理（wecom.retention.manage）。
INSERT INTO permissions (permission_code, permission_name, domain, risk_level, requires_approval, status)
VALUES
    ('wecom.view', 'View WeCom conversations (redacted)', 'compliance', 'medium', FALSE, 'active'),
    ('wecom.view_raw', 'View WeCom raw message content', 'compliance', 'critical', TRUE, 'active'),
    ('wecom.retention.manage', 'Manage WeCom retention/quality policies', 'compliance', 'high', TRUE, 'active'),
    ('wecom.sync', 'Ingest WeCom conversation sync (switch-gated)', 'compliance', 'high', FALSE, 'active')
ON CONFLICT (permission_code) DO UPDATE SET
    permission_name = EXCLUDED.permission_name,
    domain = EXCLUDED.domain,
    risk_level = EXCLUDED.risk_level,
    requires_approval = EXCLUDED.requires_approval,
    status = EXCLUDED.status;

INSERT INTO role_permissions (role_id, permission_code)
SELECT 'role_platform_admin', permission_code
  FROM permissions
 WHERE permission_code IN (
        'wecom.view',
        'wecom.view_raw',
        'wecom.retention.manage',
        'wecom.sync'
 )
ON CONFLICT (role_id, permission_code) DO NOTHING;
