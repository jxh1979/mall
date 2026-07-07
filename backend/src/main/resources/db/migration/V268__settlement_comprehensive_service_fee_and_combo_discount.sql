-- V268 结算重构收尾:综合服务费/项目管理费(§7.2 口径8+用户裁定"所有门票都抽") + 套票折扣归属(§7.4 口径10)。
--   口径:结算重构 PRD v1.0 §7.2(所有门票过平台留存这一道综合服务费,费率项目↔商户可洽谈可配)
--         + §7.4(套票折扣归属 discount_bearer:platform_absorb/merchant_absorb/split_by_ratio)。
--   范式照 V223(全 project 级可配·种子=安全默认·is_draft=TRUE 待财务复核转正·可独立回滚)。
--   ⚠️安全底线:综合服务费默认费率=0(禁硬编码);套票折扣归属默认=platform_absorb(项目承担)——
--            二者默认均【零改现有分账数值】,未配置前行为完全不变。
--   本迁移建两配置表 + 安全默认种子 + settlement_bills 加一列(综合服务费额,便于对账),不改引擎算法/现有数值,可独立回滚。

-- ① 综合服务费/项目管理费(§7.2:项目公司对【所有门票】抽的综合服务费,项目×商户×票种范围可洽谈可配)
--   落点:月度分账引擎的【专属商户直付线】(单独门票走此线);默认费率 0 → 商户仍拿全额 → 零行为变更。
--   通票的"留存 20%"是同族但另一条规则(platform_retention_rules,§5.1 第一层),本表专管【单独/专属门票】那道。
CREATE TABLE IF NOT EXISTS comprehensive_service_fee_configs (
    id                VARCHAR(48) PRIMARY KEY,
    project_id        VARCHAR(64),                       -- NULL=全局默认;非空=某项目公司特例
    merchant_id       VARCHAR(64),                       -- NULL=项目内该票种范围通用;非空=某商户单独洽谈费率
    entitlement_type  VARCHAR(64),                       -- NULL=项目内所有票种;非空=限某票种范围(如 'ticket'/'amusement')
    fee_bps           INT         NOT NULL DEFAULT 0,    -- 综合服务费万分比(默认 0=不抽;禁硬编码,后台可配)
    effective_from    DATE,
    effective_to      DATE,
    status            VARCHAR(16) NOT NULL DEFAULT 'active',
    is_draft          BOOLEAN     NOT NULL DEFAULT TRUE,
    updated_by        VARCHAR(64),
    updated_at        TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT chk_csfc_bps    CHECK (fee_bps >= 0 AND fee_bps <= 10000),
    CONSTRAINT chk_csfc_status CHECK (status IN ('active', 'inactive'))
);
-- 同 项目×商户×票种范围 唯一(用 COALESCE 归一 NULL,免多行同键)。查询精度:merchant 特例 > 项目默认。
CREATE UNIQUE INDEX IF NOT EXISTS uq_csfc ON comprehensive_service_fee_configs (
    COALESCE(project_id, '__DEFAULT__'),
    COALESCE(merchant_id, '__ANY__'),
    COALESCE(entitlement_type, '__ANY__')
);
CREATE INDEX IF NOT EXISTS idx_csfc_project ON comprehensive_service_fee_configs (project_id, merchant_id);

-- ② 套票折扣归属(§7.4:套票整体打折时折扣由谁承担,可配)
--   discount_bearer:platform_absorb(项目承担折扣·各权益按原价结算)/merchant_absorb(商户按单价占比分摊折扣)/
--                   split_by_ratio(按可配比例拆分)。默认 platform_absorb=项目承担 → 商户结算价不被折扣拉低 → 零行为变更。
--   注:当前【无折扣额数据源进结算引擎】(折扣是 TicketPricingService 定价期概念,未随核销事件下传结算),
--       故本配置+解析器先落地,逐笔折扣归属的实际应用待折扣额接入结算流水后启用(同渠道费 TODO 范式)。
CREATE TABLE IF NOT EXISTS ticket_combo_discount_configs (
    id               VARCHAR(48) PRIMARY KEY,
    project_id       VARCHAR(64),                        -- NULL=全局默认;非空=某项目公司特例
    discount_bearer  VARCHAR(24) NOT NULL DEFAULT 'platform_absorb',
    split_platform_bps INT       NOT NULL DEFAULT 0,     -- split_by_ratio 时:折扣由平台承担的万分比(商户承担=10000-此值)
    effective_from   DATE,
    effective_to     DATE,
    status           VARCHAR(16) NOT NULL DEFAULT 'active',
    is_draft         BOOLEAN     NOT NULL DEFAULT TRUE,
    updated_by       VARCHAR(64),
    updated_at       TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT chk_tcdc_bearer CHECK (discount_bearer IN ('platform_absorb', 'merchant_absorb', 'split_by_ratio')),
    CONSTRAINT chk_tcdc_split  CHECK (split_platform_bps >= 0 AND split_platform_bps <= 10000),
    CONSTRAINT chk_tcdc_status CHECK (status IN ('active', 'inactive'))
);
CREATE UNIQUE INDEX IF NOT EXISTS uq_tcdc ON ticket_combo_discount_configs (COALESCE(project_id, '__DEFAULT__'));

-- ===== 种子:安全默认(is_draft=TRUE 待复核;均【不改现有分账数值】)=====
-- 综合服务费全局默认 0%(不抽,禁硬编码;项目/商户需要时后台配非零费率)
INSERT INTO comprehensive_service_fee_configs (id, project_id, merchant_id, entitlement_type, fee_bps, is_draft)
VALUES ('csfc_default', NULL, NULL, NULL, 0, TRUE) ON CONFLICT (id) DO NOTHING;

-- 套票折扣归属全局默认 platform_absorb(项目承担 → 商户结算不受折扣影响,与现状一致)
INSERT INTO ticket_combo_discount_configs (id, project_id, discount_bearer, split_platform_bps, is_draft)
VALUES ('tcdc_default', NULL, 'platform_absorb', 10000, TRUE) ON CONFLICT (id) DO NOTHING;

-- ③ settlement_bills 加【综合服务费额】列(单独门票那道综合服务费的当月合计,项目公司收,供对账区分于通票留存)。
--   默认 0;未配置综合服务费时恒 0 → 结算单其余数值不变。加列可独立回滚。
ALTER TABLE settlement_bills ADD COLUMN IF NOT EXISTS comp_service_fee_cent BIGINT NOT NULL DEFAULT 0;

