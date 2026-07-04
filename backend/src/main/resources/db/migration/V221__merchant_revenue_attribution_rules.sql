-- V221 商户级收入归因口径可配置化(治理铁律:业务策略参数禁止硬编码)。
--   原:DashboardAggregationJob.upsertNetRevenue 把商户 net 口径写死——
--       net = 售票分量(Σallocated) + 酒吧实收 + 合同租金应收 − 售票退款 − 酒吧退款,第三方回流不计入。
--   改:各收入来源(ticket/bar/contract_rent/thirdparty)的计入方式落配置表,聚合按表决定
--       是否计入(attribution_mode)、按 bps 缩放、退款是否冲减(refund_attribution)。
--   种子=复现当前口径(net 值不变,回归):ticket 全额计入(share_bps=10000)且退款净扣,bar 全额计入退款净扣,
--       contract_rent 全额计入退款忽略,thirdparty 排除。
--   is_draft=TRUE:草案默认值,待财务/运营复核后置 FALSE 生效为正式口径。
CREATE TABLE IF NOT EXISTS merchant_revenue_attribution_rules (
    id                  VARCHAR(48) PRIMARY KEY,
    rule_scope          VARCHAR(16)  NOT NULL DEFAULT 'default',  -- default(全局默认口径) / merchant(某商户特例)
    merchant_id         VARCHAR(64),                              -- rule_scope=merchant 时必填;default 时为 NULL
    revenue_source      VARCHAR(24)  NOT NULL,                    -- ticket / bar / contract_rent / thirdparty
    attribution_mode    VARCHAR(16)  NOT NULL,                    -- gross(全额计入) / share_bps(按 bps 缩放计入) / exclude(不计入)
    share_bps           INT          NOT NULL DEFAULT 10000,      -- attribution_mode=share_bps 时的缩放万分比[0,10000]
    refund_attribution  VARCHAR(16)  NOT NULL DEFAULT 'net_deduct', -- net_deduct(退款冲减 net) / ignore(退款不冲减)
    effective_from      DATE,
    effective_to        DATE,
    status              VARCHAR(16)  NOT NULL DEFAULT 'active',    -- active / inactive
    is_draft            BOOLEAN      NOT NULL DEFAULT TRUE,        -- TRUE=草案待复核;FALSE=已复核生效口径
    updated_by          VARCHAR(64),
    updated_at          TIMESTAMPTZ  NOT NULL DEFAULT now(),
    CONSTRAINT chk_mrar_scope   CHECK (rule_scope IN ('default', 'merchant')),
    CONSTRAINT chk_mrar_source  CHECK (revenue_source IN ('ticket', 'bar', 'contract_rent', 'thirdparty')),
    CONSTRAINT chk_mrar_mode    CHECK (attribution_mode IN ('gross', 'share_bps', 'exclude')),
    CONSTRAINT chk_mrar_bps     CHECK (share_bps >= 0 AND share_bps <= 10000),
    CONSTRAINT chk_mrar_refund  CHECK (refund_attribution IN ('net_deduct', 'ignore')),
    CONSTRAINT chk_mrar_status  CHECK (status IN ('active', 'inactive')),
    CONSTRAINT chk_mrar_merchant_scope CHECK (
        (rule_scope = 'default'  AND merchant_id IS NULL) OR
        (rule_scope = 'merchant' AND merchant_id IS NOT NULL)
    )
);

-- default 口径每来源唯一(便于聚合按 revenue_source 取默认规则);merchant 特例按 (merchant_id,source) 唯一。
CREATE UNIQUE INDEX IF NOT EXISTS uq_mrar_default_source
    ON merchant_revenue_attribution_rules (revenue_source)
 WHERE rule_scope = 'default';
CREATE UNIQUE INDEX IF NOT EXISTS uq_mrar_merchant_source
    ON merchant_revenue_attribution_rules (merchant_id, revenue_source)
 WHERE rule_scope = 'merchant';

-- 种子草案(is_draft=TRUE):复现当前硬编码 net 口径,保证现有 net 值不变。
INSERT INTO merchant_revenue_attribution_rules
    (id, rule_scope, merchant_id, revenue_source, attribution_mode, share_bps, refund_attribution, is_draft)
VALUES
    ('mrar_default_ticket',        'default', NULL, 'ticket',        'share_bps', 10000, 'net_deduct', TRUE),
    ('mrar_default_bar',           'default', NULL, 'bar',           'gross',     10000, 'net_deduct', TRUE),
    ('mrar_default_contract_rent', 'default', NULL, 'contract_rent', 'gross',     10000, 'ignore',     TRUE),
    ('mrar_default_thirdparty',    'default', NULL, 'thirdparty',    'exclude',       0, 'ignore',     TRUE)
ON CONFLICT (id) DO NOTHING;
