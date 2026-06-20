-- V129：权益会话（entitlement_sessions）建表 + 会话发起/查询/领取权限点（REQ-53 收口）。
--
-- 背景（审计实证）：R4-G1 DRIFT —— 矩阵声称已补会话表 + entitlement_session_id + 会话时效 + 防错领，
-- 128 个迁移从未建 entitlement_sessions，零落地。本迁移建会话主数据表，承载附赠权益领取闭环（REQ-53）：
--   工作手机扫附赠权益码 → 发起会话（initiated）→ 一体机展示可领取（displayed）→ 领取核销（claimed，防错领+会话时效）。
-- 与相邻能力互补、不混淆：
--   - ticketing V006 issued_entitlements、amusement V082 amusement_project_entitlements 均为「模板/发放」，无 session 概念；
--   - /api/v1/terminals/offline-entitlements 是离线快照下发，非实时会话握手。
-- 域归属：放 amusement 域（一体机领取体验，与 entitlementGateway / 一体机场景一致）。
-- 跨域解耦：被扫权益以通用字符串 entitlement_ref 引用（发放 id 或权益码），不强加跨域 FK。
-- 关键：带直接 merchant_id NOT NULL 列（供 domain-ownership 自动归类 direct scope，与本域既有强制 merchant 注入一致）。

-- ── 1) 权益会话主数据表 ──────────────────────────────────────────────────────
-- status 生命周期：initiated（工作手机扫码发起）→ displayed（一体机展示可领取）→ claimed（领取核销）；
--                  expired（会话时效过期）/ cancelled（取消）为终态。
-- 防错领：claim 走条件 UPDATE WHERE status IN ('initiated','displayed') AND expires_at > now()，
--         受影响行数 == 1 才成功；过期 / 已领 / 重复领 / 已取消均拒（受影响 0 行）。
CREATE TABLE IF NOT EXISTS entitlement_sessions (
    id VARCHAR(64) PRIMARY KEY,
    session_code VARCHAR(128) NOT NULL UNIQUE,          -- 会话码（唯一，发起/展示/领取主键）
    entitlement_ref VARCHAR(128) NOT NULL,              -- 被扫的权益引用（发放 id 或权益码，跨域通用字符串）
    merchant_id VARCHAR(64) NOT NULL,                   -- 直接归属列（domain-ownership direct scope）
    project_id VARCHAR(64),                             -- 可选游乐项目（可空）
    status VARCHAR(32) NOT NULL DEFAULT 'initiated',    -- initiated|displayed|claimed|expired|cancelled
    expires_at TIMESTAMPTZ NOT NULL,                    -- 会话时效（领取必须在此之前）
    initiated_by VARCHAR(64),                           -- 工作手机操作员
    claimed_by VARCHAR(64),                             -- 领取核销操作员（可空，领取时回填）
    claimed_at TIMESTAMPTZ,                             -- 领取时间（可空，领取时回填）
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT chk_entitlement_session_status
        CHECK (status IN ('initiated', 'displayed', 'claimed', 'expired', 'cancelled'))
);

-- session_code UNIQUE 已由列约束建索引；补 merchant_id / status 查询索引。
CREATE INDEX IF NOT EXISTS idx_entitlement_sessions_merchant
    ON entitlement_sessions (merchant_id);

CREATE INDEX IF NOT EXISTS idx_entitlement_sessions_status
    ON entitlement_sessions (status);

-- ── 2) 会话发起/查询/领取权限点（domain=amusement）。 ──
-- 照 V126/V127/V128 形态：initiate/claim 为操作类（normal），view 为只读。授予 role_platform_admin。
INSERT INTO permissions (permission_code, permission_name, domain, risk_level, requires_approval, status)
VALUES
    ('amusement.entitlement_session.initiate',
     'Initiate an entitlement claim session (work phone scans bonus entitlement code)',
     'amusement', 'normal', FALSE, 'active'),
    ('amusement.entitlement_session.view',
     'View entitlement claim sessions (kiosk display / list)',
     'amusement', 'normal', FALSE, 'active'),
    ('amusement.entitlement_session.claim',
     'Claim and redeem an entitlement session (kiosk, anti-misclaim + session expiry)',
     'amusement', 'normal', FALSE, 'active')
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
        'amusement.entitlement_session.initiate',
        'amusement.entitlement_session.view',
        'amusement.entitlement_session.claim'
 )
ON CONFLICT (role_id, permission_code) DO NOTHING;
