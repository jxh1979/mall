-- V340 P0-D 提成/积分硬编码配置化(CFG-01;v7 综合审计「批 P0-1 止血」第 5 项)。
--
-- 【为什么】违反两条铁律:
--   ① 母铁律「业务策略一律可配、零硬编码全后台可改」;
--   ② DEC-CFG-ALL-NUMERIC-001(2026-07-28 用户拍板)「时间与数字一律可配、禁止写死」。
--   且 bar PRD §4/§8/§11 早已写明「卡座提成率**后台可配**(默认销售 15%/非销售 10%)」——文档说可配、代码一直写死:
--     · backend/.../bar/BarBillingService.java  calculateCommission:  int rateBp = "sales".equals(roleType) ? 1500 : 1000;
--     · backend/.../ticketing/TicketOrderService.java  POINTS_PER_YUAN_CENT = 100L(积分基础分「每元 1 分」写死)。
--   本迁移补齐这两处的配置底座,引擎随本批改为读配置。
--
-- 【零行为改变(硬要求)】两表种子 = 当前线上写死值:
--     销售 1500bp(15%)/ 非销售 1000bp(10%)/ 每积 1 分需消费 100 分(= 每 1 元积 1 分);
--   且引擎表达式与改造前逐字同形(提成 base*rateBp/10000、积分 amountCent/amountCentPerPoint,均为整数除),
--   故改造前后逐笔金额/积分严格相等。本迁移只建两张新表 + 种子,不改任何既有表/既有数据,可独立回滚(DROP 两表即可)。
--
-- 【范式】照结算配置中心 V327/V328/V331/V336(项目作用域 + 商户维 + is_draft 草案位 + 万分比 + 部分唯一索引带 status)。
--   唯一索引直接按 V329「Path 2 草案/生效并存」形态建(尾带 is_draft + WHERE status='active'),免后续再迁移;
--   种子 is_draft=FALSE(生效态)——读取侧统一强制 is_draft=FALSE,新表不走 V326 那种「插草案再一次性转正」。
--   写入面/复核转正/软删复用既有 settlement.config.manage(高敏·差人复核)与 SettlementConfigWriteService,不新增权限点。
--
-- 【缺配置行为】读取侧沿用 JdbcSettlementBillRepository.requireConfigured 的 fail-loud 口径:
--   连 __DEFAULT__ 种子都查不到(种子被删/迁移未跑到位=部署坏)则响亮报错,而非静默套回硬编码默认——
--   「无配置即响亮停」正是本次要根除硬编码回落的目的。正常态两表恒有 __DEFAULT__ 生效种子,故不触发。
--
-- 【已知遗留·不在本迁移范围】customer_points_rules.earn_ratio(V060,后台页 ADM-CUS-003「获取系数/每元积分数」)
--   当前**未接任何引擎**(全仓仅 CRUD,无读取方),属死配置;本迁移不改动它,其与本表的收敛(二选一保留)另行拍板。

-- ========== 1) 卡座销售提成费率(项目公司 × 商户 × 角色档)==========
CREATE TABLE IF NOT EXISTS bar_commission_rate_configs (
    id             VARCHAR(48) PRIMARY KEY,
    project_id     VARCHAR(64),                    -- NULL=全局默认;非空=某项目公司特例
    merchant_id    VARCHAR(64),                    -- NULL=该项目下所有商户通用;非空=某商户(酒吧)特例
    role_type      VARCHAR(16) NOT NULL,           -- 提成角色档:sales(销售人员)/ non_sales(非销售人员)
    rate_bp        INT         NOT NULL,           -- 提成率万分比(1500=15%)
    effective_from DATE,
    effective_to   DATE,
    status         VARCHAR(16) NOT NULL DEFAULT 'active',
    is_draft       BOOLEAN     NOT NULL DEFAULT FALSE,
    archive_reason TEXT,                           -- 软删铁律:下线留理由,不物理删
    updated_by     VARCHAR(64),
    updated_at     TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT chk_bcrc_role   CHECK (role_type IN ('sales', 'non_sales')),
    CONSTRAINT chk_bcrc_rate   CHECK (rate_bp >= 0 AND rate_bp <= 10000),
    CONSTRAINT chk_bcrc_status CHECK (status IN ('active', 'inactive'))
);
CREATE UNIQUE INDEX IF NOT EXISTS uq_bcrc
    ON bar_commission_rate_configs (
        COALESCE(project_id, '__DEFAULT__'),
        COALESCE(merchant_id, '__ANY__'),
        role_type,
        is_draft
    ) WHERE status = 'active';

-- 种子 = 现值(零行为改变):销售 15% / 非销售 10%,全局默认作用域、生效态。
INSERT INTO bar_commission_rate_configs (id, project_id, merchant_id, role_type, rate_bp, is_draft)
VALUES ('bcrc_default_sales',     NULL, NULL, 'sales',     1500, FALSE),
       ('bcrc_default_non_sales', NULL, NULL, 'non_sales', 1000, FALSE)
ON CONFLICT (id) DO NOTHING;

-- ========== 2) 积分累计率(项目公司 × 商户)==========
-- 口径:amount_cent_per_point = 每积 1 基础分所需的实付消费金额(单位:分)。
--   100 = 每 1 元积 1 分(现值);50 = 每 1 元积 2 分;200 = 每 2 元积 1 分。
--   选「每分所需金额」而非「每元积分数」,是为让引擎表达式与改造前的 amountCent / 100 同形(整数除),
--   凑整口径分毫不变;等级倍率/生日倍率仍在 LoyaltyService 叠加,不在本表。
CREATE TABLE IF NOT EXISTS points_accrual_rate_configs (
    id                    VARCHAR(48) PRIMARY KEY,
    project_id            VARCHAR(64),             -- NULL=全局默认
    merchant_id           VARCHAR(64),             -- NULL=所有商户通用;非空=某受理商户特例
    amount_cent_per_point BIGINT      NOT NULL,    -- 每积 1 分所需消费金额(分),须 >0
    effective_from        DATE,
    effective_to          DATE,
    status                VARCHAR(16) NOT NULL DEFAULT 'active',
    is_draft              BOOLEAN     NOT NULL DEFAULT FALSE,
    archive_reason        TEXT,
    updated_by            VARCHAR(64),
    updated_at            TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT chk_parc_unit   CHECK (amount_cent_per_point > 0),
    CONSTRAINT chk_parc_status CHECK (status IN ('active', 'inactive'))
);
CREATE UNIQUE INDEX IF NOT EXISTS uq_parc
    ON points_accrual_rate_configs (
        COALESCE(project_id, '__DEFAULT__'),
        COALESCE(merchant_id, '__ANY__'),
        is_draft
    ) WHERE status = 'active';

-- 种子 = 现值(零行为改变):每 100 分(1 元)积 1 基础分,全局默认作用域、生效态。
INSERT INTO points_accrual_rate_configs (id, project_id, merchant_id, amount_cent_per_point, is_draft)
VALUES ('parc_default', NULL, NULL, 100, FALSE)
ON CONFLICT (id) DO NOTHING;

-- ========== 3) 新表一律授权受限运行时(V296/V298/V338 血的教训:漏 GRANT → 生产静默 permission denied)==========
-- 幂等:GRANT 可重复;运行时角色缺失(部分测试环境)时跳过(与 V140/V338 同守卫)。
DO $$
BEGIN
    IF EXISTS (SELECT 1 FROM pg_roles WHERE rolname = 'complex_app') THEN
        EXECUTE 'GRANT SELECT, INSERT, UPDATE, DELETE ON bar_commission_rate_configs TO complex_app';
        EXECUTE 'GRANT SELECT, INSERT, UPDATE, DELETE ON points_accrual_rate_configs TO complex_app';
    ELSE
        RAISE NOTICE 'skip GRANT to complex_app (restricted runtime role absent in this environment)';
    END IF;
END $$;
