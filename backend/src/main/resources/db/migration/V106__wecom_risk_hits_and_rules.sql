-- P0#8 企微违规命中（合规质检命中表 + 规则码落库 + 复核队列衔接）。
-- 背景：采集侧 V087（wecom_conversations/messages/access_audits）真实落地，但：
--   1) `wecom_risk_hits` 命中表零建表——命中只写 wecom_messages.risk_flags（JSONB 标记），不入命中表、不进复核队列；
--   2) `analytics_risk_rules`（V017）仅 seed 一条 GMV_DROP（finance 域），无企微违规规则码；
--   3) WecomConversationService.detectRiskFlags 用硬编码 RISK_KEYWORDS Map（规则不可配置、不可审计）。
-- 本迁移补齐：命中表 + 企微违规规则码 seed（关键词作为规则配置数据 rule_config_json）+ 复核队列衔接列。
-- 文档：data-risk §11.4A。
--
-- 复核队列衔接口径（统一异常表 risk_alerts 由别的代理建）：
--   统一表此刻不存在；本期命中入复核写入既有孤岛 analytics_risk_alerts（V017），
--   并补可空 source_domain 列（向后兼容），企微命中统一标 source_domain='wecom'，
--   便于统一表就位后按 source_domain=wecom 口径迁移/聚合。
-- 既有列对齐（先 Read V087 / V017）：
--   wecom_conversations.id / wecom_messages.id 均 VARCHAR(64)（V087）；
--   analytics_risk_rules(rule_id/rule_code/domain/risk_level/enabled/rule_config_json/action_note，V017，
--     risk_level CHECK 仅 normal/suspicious/high/critical)；
--   analytics_risk_alerts(id/rule_id/alert_type/risk_level/object_type/object_id/summary/status/...，V017，
--     status CHECK open/investigating/resolved/dismissed，risk_level CHECK normal/suspicious/high/critical)。

-- ── 1) 企微违规命中表 ────────────────────────────────────────────────────────────
-- 一条命中 = 一条消息 × 一个规则码。关联 V087 会话/消息；matched_snippet 按既有 PiiTextRedactor 脱敏后落库。
-- status 流转：open（命中待复核）→ reviewing（已领取复核）→ confirmed（确认违规）/ dismissed（误报驳回）。
CREATE TABLE IF NOT EXISTS wecom_risk_hits (
    hit_id VARCHAR(64) PRIMARY KEY,
    conversation_id VARCHAR(64) NOT NULL REFERENCES wecom_conversations(id) ON DELETE CASCADE,
    message_id VARCHAR(64) NOT NULL REFERENCES wecom_messages(id) ON DELETE CASCADE,
    rule_code VARCHAR(128) NOT NULL,                         -- 命中的企微违规规则码（见 analytics_risk_rules）
    risk_level VARCHAR(32) NOT NULL,                         -- 命中风险等级（取自规则；normal/suspicious/high/critical）
    matched_snippet TEXT,                                    -- 命中片段（脱敏后；普通展示用，不含明文 PII）
    status VARCHAR(32) NOT NULL DEFAULT 'open',              -- open/reviewing/confirmed/dismissed
    reviewer_id VARCHAR(64),                                 -- 复核处置人（reviewing/confirmed/dismissed 时回填）
    review_note TEXT,                                        -- 复核备注（可空）
    project_id VARCHAR(64) NOT NULL,                         -- 命中所属项目（取自会话，便于项目维度风控聚合）
    alert_id VARCHAR(64),                                    -- 关联复核队列条目（analytics_risk_alerts.id；统一表就位后可改指向）
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT chk_wecom_risk_hit_level CHECK (risk_level IN ('normal', 'suspicious', 'high', 'critical')),
    CONSTRAINT chk_wecom_risk_hit_status CHECK (status IN ('open', 'reviewing', 'confirmed', 'dismissed'))
);

-- 同一消息同一规则码幂等（重复同步不产生重复命中）。
CREATE UNIQUE INDEX IF NOT EXISTS uq_wecom_risk_hits_message_rule
    ON wecom_risk_hits (message_id, rule_code);

CREATE INDEX IF NOT EXISTS idx_wecom_risk_hits_status
    ON wecom_risk_hits (status, risk_level, created_at DESC);

CREATE INDEX IF NOT EXISTS idx_wecom_risk_hits_conversation
    ON wecom_risk_hits (conversation_id, created_at DESC);

CREATE INDEX IF NOT EXISTS idx_wecom_risk_hits_project
    ON wecom_risk_hits (project_id, status, created_at DESC);

-- ── 2) 复核队列（孤岛 analytics_risk_alerts）补 source_domain 可空列 ──────────────
-- 统一异常表 risk_alerts 由别的代理建；此刻不存在，本期命中入复核写既有 analytics_risk_alerts。
-- 补可空 source_domain：标记异常来源域（wecom/finance/...），向后兼容（既有行为 NULL）；
-- 企微命中统一写 source_domain='wecom'，统一表就位后按此口径迁移/聚合。
ALTER TABLE analytics_risk_alerts
    ADD COLUMN IF NOT EXISTS source_domain VARCHAR(64);

CREATE INDEX IF NOT EXISTS idx_analytics_risk_alerts_source_domain
    ON analytics_risk_alerts (source_domain, status, created_at DESC);

-- ── 3) 企微违规规则码 seed（analytics_risk_rules，domain=compliance）────────────────
-- 由硬编码 RISK_KEYWORDS 升级为可配置规则数据：关键词作为 rule_config_json.keywords（可后台增减），
-- detectRiskFlags 改为读启用规则的 keywords 命中（启停 enabled 生效）。
-- 规则码：私单 / 私收款 / 辱骂 / 违规承诺 / 诱导加私微。
INSERT INTO analytics_risk_rules (
    rule_id, rule_code, rule_name, domain, risk_level, enabled,
    rule_config_json, action_note, created_at, updated_at
)
VALUES
    ('rule_wecom_staff_private_deal', 'STAFF_PRIVATE_DEAL', 'Staff private off-platform deal',
     'compliance', 'high', TRUE,
     '{"keywords":["私单","走私单","线下交易","私下交易"]}'::jsonb,
     'flag conversation and enqueue compliance review',
     '2026-06-19T00:00:00Z', '2026-06-19T00:00:00Z'),
    ('rule_wecom_staff_private_payment', 'STAFF_PRIVATE_PAYMENT', 'Staff private payment collection',
     'compliance', 'critical', TRUE,
     '{"keywords":["私收款","私人收款","转我","扫我码","私户","转私户"]}'::jsonb,
     'flag conversation and enqueue compliance review',
     '2026-06-19T00:00:00Z', '2026-06-19T00:00:00Z'),
    ('rule_wecom_abuse', 'ABUSE', 'Customer abuse / insult',
     'compliance', 'high', TRUE,
     '{"keywords":["辱骂","傻","滚","骂"]}'::jsonb,
     'flag conversation and enqueue compliance review',
     '2026-06-19T00:00:00Z', '2026-06-19T00:00:00Z'),
    ('rule_wecom_illegal_promise', 'ILLEGAL_PROMISE', 'Illegal / non-compliant promise',
     'compliance', 'high', TRUE,
     '{"keywords":["保证退款","稳赚","100%","包过","绝对没问题","违规承诺"]}'::jsonb,
     'flag conversation and enqueue compliance review',
     '2026-06-19T00:00:00Z', '2026-06-19T00:00:00Z'),
    ('rule_wecom_lure_private_wechat', 'LURE_PRIVATE_WECHAT', 'Lure customer to private WeChat',
     'compliance', 'high', TRUE,
     '{"keywords":["加我微信","私微","私聊","扫码加我","加我私","加个微信"]}'::jsonb,
     'flag conversation and enqueue compliance review',
     '2026-06-19T00:00:00Z', '2026-06-19T00:00:00Z')
ON CONFLICT (rule_id) DO UPDATE SET
    rule_code = EXCLUDED.rule_code,
    rule_name = EXCLUDED.rule_name,
    domain = EXCLUDED.domain,
    risk_level = EXCLUDED.risk_level,
    enabled = EXCLUDED.enabled,
    rule_config_json = EXCLUDED.rule_config_json,
    action_note = EXCLUDED.action_note,
    updated_at = EXCLUDED.updated_at;

-- ── 4) 权限 seed：企微违规命中查询 / 复核处置 ───────────────────────────────────
-- 查询复用既有 analytics.risk_alert.view 语义不够（跨域），新增 compliance 域专用权限，便于审计与最小授权。
INSERT INTO permissions (permission_code, permission_name, domain, risk_level, requires_approval, status)
VALUES
    ('wecom.risk_hit.view', 'View WeCom compliance risk hits', 'compliance', 'medium', FALSE, 'active'),
    ('wecom.risk_hit.review', 'Review / disposition WeCom compliance risk hits', 'compliance', 'high', TRUE, 'active')
ON CONFLICT (permission_code) DO UPDATE SET
    permission_name = EXCLUDED.permission_name,
    domain = EXCLUDED.domain,
    risk_level = EXCLUDED.risk_level,
    requires_approval = EXCLUDED.requires_approval,
    status = EXCLUDED.status;

INSERT INTO role_permissions (role_id, permission_code)
SELECT 'role_platform_admin', permission_code
  FROM permissions
 WHERE permission_code IN ('wecom.risk_hit.view', 'wecom.risk_hit.review')
ON CONFLICT (role_id, permission_code) DO NOTHING;
