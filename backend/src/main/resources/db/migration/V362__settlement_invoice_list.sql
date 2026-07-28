-- V362 批 4b 结算做到开票清单级(结算重构 PRD 附录 B.1 / 附录 D / 财务底稿 23_开票清单模板)。
--
-- 业务口径(业务 HTML 逐字·最终解释权 business-system-map §业务枚举「开票状态」):
--   「待开票、开票中、已开票、已红冲、已作废、开票失败;开票金额≤已收−已退−已开,退款后先红冲再重开。
--     开票由税务／电子发票第三方平台对接完成,本系统不自建开票引擎,只做状态对接与留痕。」
--   → 本迁移只建【清单】与【映射规则】两表:系统按结算单出到开票项/主体/税率/税额/凭证要件粒度交财务,
--     不接税控盘、不生成发票号、不自建开票引擎。invoice_no 仅供第三方回填留痕。
--
-- 开票主体映射(PRD 附录 D + terminology-canonical §三 2026-07-28 用户拍板口径):
--   通票 + 门票综合服务费 → 娱乐平台公司(综合体验服务费)
--   可分配池分账       → 各酒吧商户(运营服务费)
--   非散台服务费       → 各酒吧商户(娱乐服务费)
--   订台低消/二次消费/额外酒水 → 各酒吧商户(餐饮服务费,⚠️可重议;底稿旧写「中央餐饮公司」已被 07-28 裁决取代)
--   场地租金           → 长沙项目公司(= 资产公司,不动产租赁 9%)
--   游乐 → 游乐公司;演出 → 传媒公司
--   通票赠送权益兑付 = 娱乐平台公司 ↔ 提供商户【内部成本转付单,不对外开票】
--   积分 / 折扣券      = 营销成本【不开票】
--
-- 复用而非重建(不新增第三张配置表):
--   ① 开票【主体】解析复用 revenue_stream_attribution(V319,项目特例 > 平台默认 > 未配置);
--   ② 开票【税率/文建费】解析复用 settlement_tax_configs(V223,已按附录 A·C 全量种子、后台可配);
--   ③ 本表只补两者都没有的部分:发票项目名称 / 税目口径 / 凭证要件 / 是否对外开票。
--
-- 本迁移只建两张新表 + 两个权限点,不改任何既有表的列或数据、可独立回滚。

-- ========== ① 开票主体×项目×税率映射规则(PRD 附录 B.1 `invoice_subject_rules`) ==========
-- 附录 A 抬头:「税务属性/开票映射为规则性」——故本表为规则性种子,不入配置中心草案/复核流;
-- 真正需要按项目谈判改动的费率在 settlement_tax_configs(已可后台改)。
CREATE TABLE IF NOT EXISTS invoice_subject_rules (
    id                VARCHAR(48) PRIMARY KEY,
    project_id        VARCHAR(64),                       -- NULL=平台默认;非空=项目公司特例
    income_type       VARCHAR(32) NOT NULL,              -- 与 settlement_tax_configs.income_type 同一枚举
    revenue_stream    VARCHAR(32),                       -- 非空=按该收入流查 revenue_stream_attribution(V355 九条流)定主体:
                                                         --   beneficiary_kind=entity → 用配置的主体;
                                                         --   beneficiary_kind=per_merchant / 未配置 → 回落该笔分账的商户收款主体。
                                                         -- NULL=该收入类型在归属层没有对应收入流(如可分配池分账),直接按商户收款主体
    invoice_item_name VARCHAR(64)  NOT NULL,             -- 发票项目名称(底稿 23「发票项目名称」列)
    tax_category      VARCHAR(64)  NOT NULL,             -- 税目口径(底稿 23「税目口径」列)
    evidence_required VARCHAR(255) NOT NULL DEFAULT '',  -- 凭证要件(底稿 23「单据附件」列)
    invoiceable       BOOLEAN      NOT NULL DEFAULT TRUE,-- FALSE=内部转付/营销成本,不进对外开票清单
    status            VARCHAR(16)  NOT NULL DEFAULT 'active',
    updated_by        VARCHAR(64),
    updated_at        TIMESTAMPTZ  NOT NULL DEFAULT now(),
    CONSTRAINT chk_isr_status CHECK (status IN ('active', 'inactive'))
);

-- 同 (项目, 收入类型) 至多一条 active 规则(平台默认 project_id=NULL 归一到 __DEFAULT__ 保持唯一)。
CREATE UNIQUE INDEX IF NOT EXISTS uq_isr_project_income
    ON invoice_subject_rules (COALESCE(project_id, '__DEFAULT__'), income_type)
 WHERE status = 'active';

COMMENT ON TABLE invoice_subject_rules IS
    '开票主体×项目×税率映射规则(PRD 附录 D / 底稿 23_开票清单模板);主体解析复用 revenue_stream_attribution,费率复用 settlement_tax_configs';

-- 种子:底稿 23_开票清单模板逐行 + 2026-07-28 主体归属裁决(订台/二消/额外酒水归各酒吧商户,非底稿旧写的中央餐饮)。
INSERT INTO invoice_subject_rules
    (id, income_type, revenue_stream, invoice_item_name, tax_category, evidence_required, invoiceable)
-- 收入流取 V355 九条枚举:门票综合服务费走专列 ticket_service_fee(V355 已说明:综合服务费覆盖所有门票项目,
-- 与通票收入 all_pass_ticket 归属恰好同为娱乐平台公司是业务巧合、不是同一笔钱,合并会让日后其一改归属时另一个被连坐)。
-- 可分配池分账在归属层没有对应收入流(它是通票收入扣完各项后分给酒吧的部分),故 revenue_stream 留空按商户收款主体。
VALUES
    ('isr_ticket',    'ticket',         'ticket_service_fee',
     '综合体验服务费',   '综合体验/娱乐平台服务', '购票页截图、用户协议、对账单',        TRUE),
    ('isr_spvsvc',    'spv_service',    NULL,
     '运营服务费',       '运营服务',             '服务协议、对账单、服务成果确认单',    TRUE),
    ('isr_nonstd',    'nonstd_service', 'bar_service_fee',
     '娱乐服务费',       '娱乐服务',             '消费小票、服务费台账',                TRUE),
    ('isr_catering',  'catering',       'table_dining',
     '餐饮服务费',       '餐饮服务',             '点单单、消费小票、吧台出品单',        TRUE),
    ('isr_rent',      'rent',           'venue_rent',
     '不动产租赁服务费', '不动产租赁',           '商铺租赁合同、对账单',                TRUE),
    ('isr_game',      'game',           'amusement_ticket',
     '游乐服务费',       '游乐服务',             '票面/核销规则、对账单',               TRUE),
    ('isr_show',      'show',           'performance_ticket',
     '文化服务费',       '文化服务/酒吧演艺',     '演出合同、票务规则',                  TRUE),
    -- 底稿 23 第 12 行:通票赠送权益兑付 = 娱乐平台公司↔提供商户内部成本转付单,不对外开票。
    ('isr_giveaway',  'giveaway_transfer', NULL,
     '内部领用单/成本转付单', '内部成本转付',     '核销记录、内部结算价快照',            FALSE),
    -- 底稿 23 第 11 行:会员积分/门票折扣券 = 营销成本,不开票。
    ('isr_marketing', 'marketing',      NULL,
     '不直接开票(会员权益准备)', '会员营销成本/积分准备', '',                          FALSE)
ON CONFLICT (id) DO NOTHING;

-- ========== ② 开票清单条目(PRD 附录 B.1 `settlement_invoice_items`) ==========
CREATE TABLE IF NOT EXISTS settlement_invoice_items (
    id                     VARCHAR(64)  PRIMARY KEY,
    bill_id                VARCHAR(64)  NOT NULL REFERENCES settlement_bills(id),
    settlement_item_id     VARCHAR(64),                  -- 池分账行来源;NULL=结算单头级(如门票综合服务费)
    project_id             VARCHAR(64),
    period_start           DATE         NOT NULL,
    period_end             DATE         NOT NULL,
    income_type            VARCHAR(32)  NOT NULL,
    -- 开票主体:business_entities.id(经 revenue_stream_attribution 或商户结算主体解析);
    -- 归属未配置时为 NULL —— 清单仍出、页面标「归属未配置」提示去配,不静默丢账。
    subject_entity_id      VARCHAR(64),
    subject_name           VARCHAR(120),                 -- 主体名快照(财务直接看名字,不必再关联查)
    invoice_item_name      VARCHAR(64)  NOT NULL,        -- 发票项目名称快照
    tax_category           VARCHAR(64),                  -- 税目口径快照
    evidence_required      VARCHAR(255),                 -- 凭证要件快照
    amount_cent            BIGINT       NOT NULL,        -- 开票金额(含税),整数分
    vat_rate_bps           INT          NOT NULL DEFAULT 0,   -- 增值税率快照(万分比,600=6%)
    vat_amount_cent        BIGINT       NOT NULL DEFAULT 0,   -- 税额 = 含税额 × 率 ÷ (10000+率),四舍五入到分
    cultural_fee_applicable BOOLEAN     NOT NULL DEFAULT FALSE,-- 文化事业建设费标记
    cultural_fee_rate_bps  INT          NOT NULL DEFAULT 0,
    cultural_fee_cent      BIGINT       NOT NULL DEFAULT 0,
    status                 VARCHAR(24)  NOT NULL DEFAULT 'pending',
    invoice_no             VARCHAR(64),                  -- 第三方开票平台回填的发票号(本系统不生成)
    remark                 VARCHAR(500),
    created_by             VARCHAR(64),
    updated_by             VARCHAR(64),
    created_at             TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at             TIMESTAMPTZ  NOT NULL DEFAULT now(),
    -- 业务 HTML「开票状态」六态逐字:待开票 pending / 开票中 issuing / 已开票 issued /
    -- 已红冲 reversed / 已作废 voided / 开票失败 failed。
    CONSTRAINT chk_sii_status CHECK (status IN ('pending','issuing','issued','reversed','voided','failed')),
    CONSTRAINT chk_sii_amount CHECK (amount_cent > 0),
    CONSTRAINT chk_sii_rate   CHECK (vat_rate_bps >= 0 AND vat_rate_bps <= 10000
                                 AND cultural_fee_rate_bps >= 0 AND cultural_fee_rate_bps <= 10000),
    CONSTRAINT chk_sii_period CHECK (period_end >= period_start)
);

-- 「开票金额 ≤ 已收 − 已退 − 已开」的 DB 侧兜底:同一来源(结算单×收入类型×分账行)在【占额】状态下
-- 至多一条 —— 挡并发重复生成。红冲/作废后该键释放,可再生成(= HTML「退款后先红冲再重开」)。
-- 金额上限本身由服务层按 应开额 − 已开额 计算(跨行聚合,非单行约束能表达)。
CREATE UNIQUE INDEX IF NOT EXISTS uq_sii_source_open
    ON settlement_invoice_items (bill_id, income_type, COALESCE(settlement_item_id, '__BILL__'))
 WHERE status IN ('pending', 'issuing', 'issued');

CREATE INDEX IF NOT EXISTS idx_sii_period_subject
    ON settlement_invoice_items (period_start, subject_entity_id);
CREATE INDEX IF NOT EXISTS idx_sii_project_status
    ON settlement_invoice_items (project_id, status);

COMMENT ON TABLE settlement_invoice_items IS
    '开票清单条目(批 4b·底稿 23_开票清单模板粒度):只生成清单交财务,不对接税控盘、不自建开票引擎';
COMMENT ON COLUMN settlement_invoice_items.invoice_no IS
    '第三方税务/电子发票平台回填的发票号;本系统不生成发票号';

-- ========== ③ 权限点 ==========
INSERT INTO permissions (permission_code, permission_name, domain, risk_level, requires_approval, status)
VALUES
    ('settlement.invoice.view',   '查看开票清单(按主体分组·含税率税额)', 'settlement', 'medium', FALSE, 'active'),
    ('settlement.invoice.manage', '生成/红冲/作废开票清单条目(高敏)',     'settlement', 'high',   TRUE,  'active')
ON CONFLICT (permission_code) DO NOTHING;

INSERT INTO role_permissions (role_id, permission_code)
SELECT v.role_id, v.permission_code
FROM (VALUES
    ('role_platform_admin', 'settlement.invoice.view'),
    ('role_platform_admin', 'settlement.invoice.manage'),
    ('role_ops_manager',    'settlement.invoice.view')
) AS v(role_id, permission_code)
ON CONFLICT (role_id, permission_code) DO NOTHING;

-- ========== ④ 生产受限运行时权限(V298/V299 教训:新表必须显式 GRANT 给 complex_app,否则生产 500) ==========
DO $$
BEGIN
    IF EXISTS (SELECT 1 FROM pg_roles WHERE rolname = 'complex_app') THEN
        GRANT SELECT, INSERT, UPDATE, DELETE ON invoice_subject_rules TO complex_app;
        GRANT SELECT, INSERT, UPDATE, DELETE ON settlement_invoice_items TO complex_app;
    END IF;
END
$$;
