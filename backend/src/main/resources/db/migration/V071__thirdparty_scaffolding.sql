-- R5 T14 第三方接口/开关预留骨架（裁决③：凡第三方一律预留接口 + 后台开关控制开放、不阻塞其它功能）。
-- 本迁移落三张硬缺口表 + 显式 enabled=false 的终端能力开关行（默认关闭）+ 两个权限点。
-- 口径：仅边界预留——机器人被动留痕、视频点位时间锚点 + 调阅审计；真实回流/自动对账待厂商资料 + 开关开启后交付，本期不伪造对账。
-- 依据：docs/r5-spec-T14-thirdparty-scaffolding-dev-design-20260618.md；现状取自 V015（integration_provider_adapters）与 V010/V068（terminal_capability_switches）。

-- ── 1) 机器人被动留痕：写回调（现状仅只读端点）—— 与 T17 dispatch 段对接 ─────────
CREATE TABLE IF NOT EXISTS integration_robot_task_events (
    id VARCHAR(64) PRIMARY KEY,
    robot_task_ref VARCHAR(128) NOT NULL,
    event_type VARCHAR(32) NOT NULL,      -- created/picked/delivering/delivered/failed
    handover_id VARCHAR(64),              -- 预留：关联 T17 交接
    payload JSONB NOT NULL DEFAULT '{}',
    reported_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT chk_robot_event_type CHECK (event_type IN ('created', 'picked', 'delivering', 'delivered', 'failed'))
);

CREATE INDEX IF NOT EXISTS idx_integration_robot_task_events_ref
    ON integration_robot_task_events (robot_task_ref, reported_at DESC);

-- ── 2) 视频监控"点位 + 时间锚点"预留 + 调阅审计 ─────────────────────────────────
CREATE TABLE IF NOT EXISTS integration_video_anchors (
    id VARCHAR(64) PRIMARY KEY,
    point_id VARCHAR(64) NOT NULL,
    event_time TIMESTAMPTZ NOT NULL,
    business_ref VARCHAR(128),            -- 关联业务动作（核销/交接/异常）
    clip_index_ref VARCHAR(256),          -- 后续匹配的监控片段索引（厂商接入后填，本期留空）
    created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX IF NOT EXISTS idx_integration_video_anchors_point
    ON integration_video_anchors (point_id, event_time DESC);

CREATE TABLE IF NOT EXISTS integration_video_access_audits (
    id VARCHAR(64) PRIMARY KEY,
    anchor_id VARCHAR(64) REFERENCES integration_video_anchors(id),
    viewer_user_id VARCHAR(64) NOT NULL,
    approval_ref VARCHAR(64),
    viewed_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX IF NOT EXISTS idx_integration_video_access_audits_anchor
    ON integration_video_access_audits (anchor_id, viewed_at DESC);

-- ── 3) 终端能力开关：各第三方 integration.{type}.sync 显式 enabled=false（默认关闭） ──
-- 注意：T2 TerminalSwitchGuard.resolveEffectiveSwitch 语义为"无显式行=默认开启"（opt-out）。
--   因此第三方默认关闭必须显式插入 enabled=false 行；terminal_type='*'（跨端通配）、scope_type='global'。
--   取值集符合 V068 CHECK（terminal_type 含 '*'、scope_type 含 'global'）。ON CONFLICT 幂等。
INSERT INTO terminal_capability_switches
    (id, terminal_type, capability_code, scope_type, scope_id, enabled, reason, created_at, updated_at)
VALUES
    ('tcs_int_catering_pos_off',  '*', 'integration.catering_pos.sync',   'global', 'global', FALSE, 'R5 T14 第三方默认关闭，待厂商接入 + 后台开启', now(), now()),
    ('tcs_int_retail_pos_off',    '*', 'integration.retail_pos.sync',     'global', 'global', FALSE, 'R5 T14 第三方默认关闭，待厂商接入 + 后台开启', now(), now()),
    ('tcs_int_robot_off',         '*', 'integration.robot.sync',          'global', 'global', FALSE, 'R5 T14 第三方默认关闭，待厂商接入 + 后台开启', now(), now()),
    ('tcs_int_video_off',         '*', 'integration.video.sync',          'global', 'global', FALSE, 'R5 T14 第三方默认关闭，待厂商接入 + 后台开启', now(), now()),
    ('tcs_int_wecom_off',         '*', 'integration.wecom.sync',          'global', 'global', FALSE, 'R5 T14 第三方默认关闭，待厂商接入 + 后台开启', now(), now()),
    ('tcs_int_ticket_chan_off',   '*', 'integration.ticket_channel.sync', 'global', 'global', FALSE, 'R5 T14 第三方默认关闭，待厂商接入 + 后台开启', now(), now()),
    ('tcs_int_payment_off',       '*', 'integration.payment.sync',        'global', 'global', FALSE, 'R5 T14 第三方默认关闭，待厂商接入 + 后台开启', now(), now()),
    ('tcs_int_face_age_off',      '*', 'integration.face_age.sync',       'global', 'global', FALSE, 'R5 T14 第三方默认关闭，待厂商接入 + 后台开启', now(), now())
ON CONFLICT (terminal_type, capability_code, scope_type, scope_id) DO NOTHING;

-- ── 4) 权限点 seed（integration.video.view 调阅敏感；integration.adapter.write 适配器写入） ──
INSERT INTO permissions (permission_code, permission_name, domain, risk_level, requires_approval, status)
VALUES
    ('integration.video.view',    'View integration video anchors (sensitive, audited)', 'integration', 'high',   TRUE,  'active'),
    ('integration.adapter.write', 'Write integration third-party adapter scaffolding',    'integration', 'high',   TRUE,  'active')
ON CONFLICT (permission_code) DO UPDATE SET
    permission_name = EXCLUDED.permission_name,
    domain = EXCLUDED.domain,
    risk_level = EXCLUDED.risk_level,
    requires_approval = EXCLUDED.requires_approval,
    status = EXCLUDED.status;

INSERT INTO role_permissions (role_id, permission_code)
SELECT 'role_platform_admin', permission_code
  FROM permissions
 WHERE permission_code IN ('integration.video.view', 'integration.adapter.write')
ON CONFLICT (role_id, permission_code) DO NOTHING;
