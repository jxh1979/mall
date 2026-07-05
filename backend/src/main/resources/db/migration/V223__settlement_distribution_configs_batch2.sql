-- V223 结算重构批2-1:通票分账配置表族(留存/分配方式/排名档位/每客·固定额/赠酒/税费)。
--   口径:结算重构 PRD v1.0 §5.0 全流程 + §5.1 三种分配方式 + 附录C 税费全可配。
--   全部 project 级可配,种子=默认值(is_draft=TRUE 待财务复核转正,与 V221 同范式)。
--   本迁移仅建配置表 + 种子,不改任何现有表/逻辑,可独立回滚。分账引擎(读这些配置)在后续步骤。

-- ① 项目公司留存比例(§5.0 步骤⑤:可分配池 × 留存% 归项目,剩余给各酒吧)
CREATE TABLE IF NOT EXISTS platform_retention_rules (
    id             VARCHAR(48) PRIMARY KEY,
    project_id     VARCHAR(64),                       -- NULL=全局默认;非空=某项目公司特例
    retention_bps  INT         NOT NULL DEFAULT 2000, -- 留存万分比(2000=20%,可配)
    effective_from DATE,
    effective_to   DATE,
    status         VARCHAR(16) NOT NULL DEFAULT 'active',
    is_draft       BOOLEAN     NOT NULL DEFAULT TRUE,
    updated_by     VARCHAR(64),
    updated_at     TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT chk_prr_bps    CHECK (retention_bps >= 0 AND retention_bps <= 10000),
    CONSTRAINT chk_prr_status CHECK (status IN ('active', 'inactive'))
);
CREATE UNIQUE INDEX IF NOT EXISTS uq_prr ON platform_retention_rules (COALESCE(project_id, '__DEFAULT__'));

-- ② 赠酒成本(§5.0 步骤③:核销杯数 × 每杯成本,先留补偿酒吧酒成本)
CREATE TABLE IF NOT EXISTS giveaway_cost_configs (
    id                VARCHAR(48) PRIMARY KEY,
    project_id        VARCHAR(64),
    cost_per_cup_cent BIGINT      NOT NULL DEFAULT 300,  -- 每杯赠酒成本(分,300=3元,可配)
    effective_from    DATE,
    effective_to      DATE,
    status            VARCHAR(16) NOT NULL DEFAULT 'active',
    is_draft          BOOLEAN     NOT NULL DEFAULT TRUE,
    updated_by        VARCHAR(64),
    updated_at        TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT chk_gcc_cost   CHECK (cost_per_cup_cent >= 0),
    CONSTRAINT chk_gcc_status CHECK (status IN ('active', 'inactive'))
);
CREATE UNIQUE INDEX IF NOT EXISTS uq_gcc ON giveaway_cost_configs (COALESCE(project_id, '__DEFAULT__'));

-- ③ 各酒吧分账方式主配置(§5.1:project 级选一种分配方式)
CREATE TABLE IF NOT EXISTS ticket_distribution_configs (
    id             VARCHAR(48) PRIMARY KEY,
    project_id     VARCHAR(64),
    method_type    VARCHAR(24) NOT NULL DEFAULT 'by_rank_tier', -- by_venue_per_guest / by_rank_tier / by_fixed_amount
    effective_from DATE,
    effective_to   DATE,
    status         VARCHAR(16) NOT NULL DEFAULT 'active',
    is_draft       BOOLEAN     NOT NULL DEFAULT TRUE,
    updated_by     VARCHAR(64),
    updated_at     TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT chk_tdc_method CHECK (method_type IN ('by_venue_per_guest','by_rank_tier','by_fixed_amount')),
    CONSTRAINT chk_tdc_status CHECK (status IN ('active', 'inactive'))
);
CREATE UNIQUE INDEX IF NOT EXISTS uq_tdc ON ticket_distribution_configs (COALESCE(project_id, '__DEFAULT__'));

-- ④ by_rank_tier 排名档位(§5.0 例:第1名16%/2-4名各12%/5-8名各9%/后2名各6%,可配)
CREATE TABLE IF NOT EXISTS ticket_distribution_rank_tiers (
    id          VARCHAR(48) PRIMARY KEY,
    project_id  VARCHAR(64),                 -- NULL=默认档位
    rank_from   INT         NOT NULL,        -- 排名区间起(含),1=第1名
    rank_to     INT         NOT NULL,        -- 排名区间止(含)
    share_bps   INT         NOT NULL,        -- 该区间每家分得 各酒吧总池 的万分比
    status      VARCHAR(16) NOT NULL DEFAULT 'active',
    is_draft    BOOLEAN     NOT NULL DEFAULT TRUE,
    updated_by  VARCHAR(64),
    updated_at  TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT chk_trt_rank  CHECK (rank_from >= 1 AND rank_to >= rank_from),
    CONSTRAINT chk_trt_bps   CHECK (share_bps >= 0 AND share_bps <= 10000),
    CONSTRAINT chk_trt_status CHECK (status IN ('active', 'inactive'))
);
CREATE INDEX IF NOT EXISTS idx_trt_project_rank ON ticket_distribution_rank_tiers (project_id, rank_from);

-- ⑤ by_venue_per_guest 每客单价 / by_fixed_amount 固定额(按商户配)
CREATE TABLE IF NOT EXISTS ticket_distribution_merchant_rates (
    id                VARCHAR(48) PRIMARY KEY,
    project_id        VARCHAR(64),
    merchant_id       VARCHAR(64) NOT NULL,
    per_guest_cent    BIGINT,                 -- by_venue_per_guest:每进一客给几元(分)
    fixed_amount_cent BIGINT,                 -- by_fixed_amount:每周期固定额(分)
    status            VARCHAR(16) NOT NULL DEFAULT 'active',
    is_draft          BOOLEAN     NOT NULL DEFAULT TRUE,
    updated_by        VARCHAR(64),
    updated_at        TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT chk_tdmr_status CHECK (status IN ('active', 'inactive')),
    CONSTRAINT chk_tdmr_nonneg CHECK ((per_guest_cent IS NULL OR per_guest_cent >= 0)
                                  AND (fixed_amount_cent IS NULL OR fixed_amount_cent >= 0))
);
CREATE UNIQUE INDEX IF NOT EXISTS uq_tdmr ON ticket_distribution_merchant_rates (COALESCE(project_id, '__DEFAULT__'), merchant_id);

-- ⑥ 税费配置(附录C:每主体×收入类型×税种 有无+费率,全可配)
CREATE TABLE IF NOT EXISTS settlement_tax_configs (
    id                    VARCHAR(48) PRIMARY KEY,
    project_id            VARCHAR(64),
    income_type           VARCHAR(32) NOT NULL,  -- ticket/spv_service/nonstd_service/catering/game/show/rent
    tax_type              VARCHAR(24) NOT NULL,  -- vat(增值税)/cultural_fee(文化事业建设费)/surcharge(附加税)
    applicable            BOOLEAN     NOT NULL DEFAULT TRUE,  -- 有没有这笔费用(可后台填)
    rate_bps              INT         NOT NULL DEFAULT 0,     -- 费率万分比(600=6%,300=3%)
    effective_from        DATE,
    effective_to          DATE,
    status                VARCHAR(16) NOT NULL DEFAULT 'active',
    is_draft              BOOLEAN     NOT NULL DEFAULT TRUE,
    updated_by            VARCHAR(64),
    updated_at            TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT chk_stc_tax    CHECK (tax_type IN ('vat','cultural_fee','surcharge')),
    CONSTRAINT chk_stc_rate   CHECK (rate_bps >= 0 AND rate_bps <= 10000),
    CONSTRAINT chk_stc_status CHECK (status IN ('active', 'inactive'))
);
CREATE UNIQUE INDEX IF NOT EXISTS uq_stc ON settlement_tax_configs (COALESCE(project_id, '__DEFAULT__'), income_type, tax_type);

-- ===== 种子:默认口径(is_draft=TRUE 待复核)=====
INSERT INTO platform_retention_rules (id, project_id, retention_bps, is_draft)
VALUES ('prr_default', NULL, 2000, TRUE) ON CONFLICT (id) DO NOTHING;

INSERT INTO giveaway_cost_configs (id, project_id, cost_per_cup_cent, is_draft)
VALUES ('gcc_default', NULL, 300, TRUE) ON CONFLICT (id) DO NOTHING;

INSERT INTO ticket_distribution_configs (id, project_id, method_type, is_draft)
VALUES ('tdc_default', NULL, 'by_rank_tier', TRUE) ON CONFLICT (id) DO NOTHING;

-- 默认排名档位(§5.0 例:10 家=第1名16%+2-4名各12%+5-8名各9%+后2名各6%=100%)
INSERT INTO ticket_distribution_rank_tiers (id, project_id, rank_from, rank_to, share_bps, is_draft)
VALUES ('trt_d1', NULL, 1, 1, 1600, TRUE),
       ('trt_d2', NULL, 2, 4,  1200, TRUE),
       ('trt_d3', NULL, 5, 8,   900, TRUE),
       ('trt_d4', NULL, 9, 10,  600, TRUE)
ON CONFLICT (id) DO NOTHING;

-- 税费默认(附录C:通票/服务费/娱乐 计增值税6%+文建费3%;餐饮/游戏 6%;演出文建费默认不计;租赁9%)
INSERT INTO settlement_tax_configs (id, project_id, income_type, tax_type, applicable, rate_bps, is_draft)
VALUES ('stc_ticket_vat',      NULL, 'ticket',        'vat',          TRUE, 600, TRUE),
       ('stc_ticket_cf',       NULL, 'ticket',        'cultural_fee', TRUE, 300, TRUE),
       ('stc_spvsvc_vat',      NULL, 'spv_service',   'vat',          TRUE, 600, TRUE),
       ('stc_nonstd_vat',      NULL, 'nonstd_service','vat',          TRUE, 600, TRUE),
       ('stc_nonstd_cf',       NULL, 'nonstd_service','cultural_fee', TRUE, 300, TRUE),
       ('stc_catering_vat',    NULL, 'catering',      'vat',          TRUE, 600, TRUE),
       ('stc_game_vat',        NULL, 'game',          'vat',          TRUE, 600, TRUE),
       ('stc_game_cf',         NULL, 'game',          'cultural_fee', TRUE, 300, TRUE),  -- 游艺计,大型游乐可后台关
       ('stc_show_vat',        NULL, 'show',          'vat',          TRUE, 600, TRUE),
       ('stc_show_cf',         NULL, 'show',          'cultural_fee', FALSE,300, TRUE),  -- 演出=文化服务,默认不计(可开)
       ('stc_rent_vat',        NULL, 'rent',          'vat',          TRUE, 900, TRUE)
ON CONFLICT (id) DO NOTHING;
