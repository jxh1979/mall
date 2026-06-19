-- V093 支付真实渠道：预留接口 + 能力开关（默认关，后台可配）。
-- 用户口径：第三方/支付一律"预留接口（端口/适配器）+ 能力开关默认关；开关后台可配；
--   关→走现有内部/占位结算（不接渠道、不阻塞）；开→路由到渠道适配器（当前无真实厂商=占位 NOT_CONFIGURED，留接缝）"。
-- 涉及业务：finance 支付意图 / finance 退款、储值退款打款、付费会员自动续费扣款（均经 PaymentGatewayPort）。
-- 事实源对齐：terminal_capability_switches（R5 T2，V068 CHECK 含 terminal_type '*' 与 scope 'global'）、
--   finance_payment_intents（V008）、permissions/role_permissions（既有 RBAC）。

-- ── 1) 能力开关：payment.gateway.enabled 显式 seed = FALSE（默认关） ──────────────
-- 注意：T2 TerminalSwitchGuard / resolveEffectiveSwitch 语义为"无显式行=默认开启"（opt-out）。
--   支付渠道要求"默认关"，故必须显式插入 enabled=FALSE 行；terminal_type='*'（跨端通配）、scope_type='global'。
-- 为何独立 payment.gateway.enabled 而非复用 integration.payment.sync（V071）：
--   integration.payment.sync 语义=第三方支付对账/数据同步；payment.gateway.enabled 语义=收款/退款/自动续费
--   是否经真实支付渠道结算。二者职责不同，独立开关使后台配置与审计语义明确，互不牵连。
INSERT INTO terminal_capability_switches
    (id, terminal_type, capability_code, scope_type, scope_id, enabled, reason, created_at, updated_at)
VALUES
    ('tcs_payment_gateway_off', '*', 'payment.gateway.enabled', 'global', 'global', FALSE,
     '支付真实渠道默认关闭：关时走内部/占位结算（reserved，人工/内部确认，不阻塞）；'
     || '待真实渠道 SDK 接入 + 后台 updateCapabilitySwitch 开启后路由至渠道适配器', now(), now())
ON CONFLICT (terminal_type, capability_code, scope_type, scope_id) DO NOTHING;

-- ── 2) finance_payment_intents 渠道交互预留字段 ─────────────────────────────────
-- 记录与真实支付渠道的交互预留：开关关时为占位（reserved / RESERVED）；开关开经 PaymentGatewayPort 后由适配器回填。
ALTER TABLE finance_payment_intents
    ADD COLUMN IF NOT EXISTS gateway_provider VARCHAR(64),
    ADD COLUMN IF NOT EXISTS gateway_ref      VARCHAR(128),
    ADD COLUMN IF NOT EXISTS gateway_status   VARCHAR(32);

-- ── 3) 权限 seed：finance.payment_gateway.manage（后台配支付渠道开关/适配器） ──────
INSERT INTO permissions (permission_code, permission_name, domain, risk_level, requires_approval, status)
VALUES
    ('finance.payment_gateway.manage', 'Manage payment gateway capability switch and adapters',
     'finance', 'high', TRUE, 'active')
ON CONFLICT (permission_code) DO UPDATE SET
    permission_name = EXCLUDED.permission_name,
    domain = EXCLUDED.domain,
    risk_level = EXCLUDED.risk_level,
    requires_approval = EXCLUDED.requires_approval,
    status = EXCLUDED.status;

INSERT INTO role_permissions (role_id, permission_code)
SELECT 'role_platform_admin', permission_code
  FROM permissions
 WHERE permission_code IN ('finance.payment_gateway.manage')
ON CONFLICT (role_id, permission_code) DO NOTHING;
