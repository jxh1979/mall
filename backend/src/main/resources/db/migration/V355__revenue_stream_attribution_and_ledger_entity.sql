-- V355 通票分账瀑布重建 S1(主体与归属层)·第 2 刀:
--   A. 归属配置层扩容 —— revenue_stream_attribution 加「收款方类型」+ 收入流扩到 9 条;
--   B. 账本层补主体列 —— settlement_transactions 加 beneficiary_entity_id(方案 §十六 B-7 硬前置)。
--
-- 【业务依据】business-enum-quick-reference §「六业态收入归属」逐条:
--     通票收入 与 门票综合服务费 → 娱乐平台公司
--     订台低消 / 二次消费 / 额外酒水 → **各酒吧商户**
--     非散台服务费                   → **各酒吧商户**
--     向商户供货的货款               → 中央餐饮公司
--     游乐票                         → 游乐公司
--     演出票                         → 传媒公司
--     场地租金                       → 长沙项目公司(即资产公司)
--   business-system-map §「通票收入分账全流程」:赠送权益预估成本 → 娱乐平台公司;
--   同上 §「通票结算口径」:未核销权益款 → 娱乐平台公司,游乐通票的未核销权益款 → 游乐公司。
--
-- 【零金额影响】本迁移只加列 / 放宽约束 / 扩枚举,不写入任何归属配置行:
--   归属未配置时解析仍返 NULL(阻断开关 settlement.attribution.fail-loud.enabled 默认 false),
--   引擎逐分不变。归属行由运营在「结算配置中心」按草案 → 复核转正的正路落,留痕可查。

-- ============================================================================
-- A. revenue_stream_attribution:收款方类型 + 9 条收入流
-- ============================================================================
--
-- 为什么必须加 beneficiary_kind:新口径里有两类收款方,老表只能表达第一类——
--   ① 归**某一个主体**(通票收入→娱乐平台公司、场地租金→长沙项目公司);
--   ② 归**产生这笔收入的各商户自己**(订台低消 / 二次消费 / 额外酒水、非散台服务费 → 各酒吧商户)。
-- ② 没有单一收款主体,旧表 management_entity_id NOT NULL 逼着运营随便填一个主体,
--    对账时会把「本该归各酒吧的钱」显示成归某个管理公司 —— 这是会误导财务的错标注,不是小事。
ALTER TABLE revenue_stream_attribution
    ADD COLUMN IF NOT EXISTS beneficiary_kind VARCHAR(16) NOT NULL DEFAULT 'entity';

COMMENT ON COLUMN revenue_stream_attribution.beneficiary_kind IS
    '收款方类型:entity=归某个经营主体(management_entity_id 指向 business_entities);per_merchant=归产生该笔收入的各商户自己(无单一主体,主体列须为空)';

ALTER TABLE revenue_stream_attribution
    ALTER COLUMN management_entity_id DROP NOT NULL;

COMMENT ON COLUMN revenue_stream_attribution.management_entity_id IS
    '收款主体 business_entities.id;beneficiary_kind=entity 时必填(可指 L2 项目公司或 L3 业态主体,场地租金即归 L2 长沙项目公司),per_merchant 时须为空';

-- 收入流 6 → 9:补「门票综合服务费」「赠送权益预估成本」「向商户供货的货款」三条。
--   ticket_service_fee 单列而不复用 all_pass_ticket:综合服务费覆盖**所有门票项目**(不止通票),
--   两者归属恰好同为娱乐平台公司是业务巧合,不是同一笔钱,合并会让日后其一改归属时另一个被连坐。
ALTER TABLE revenue_stream_attribution DROP CONSTRAINT IF EXISTS chk_rsa_stream;
ALTER TABLE revenue_stream_attribution ADD CONSTRAINT chk_rsa_stream CHECK (revenue_stream IN (
    'all_pass_ticket',      -- 通票收入
    'table_dining',         -- 订台低消 / 二次消费 / 额外酒水
    'bar_service_fee',      -- 非散台(包厢/卡座)服务费
    'amusement_ticket',     -- 游乐票与游乐核销收入
    'performance_ticket',   -- 演出票
    'venue_rent',           -- 场地租金 / 商业租金
    'ticket_service_fee',   -- 门票综合服务费(= 项目管理费 / 娱乐平台公司留存,同一项只扣一次)
    'pass_gift_cost',       -- 通票赠送权益预估成本
    'supply_goods'));       -- 向商户供货的货款

ALTER TABLE revenue_stream_attribution DROP CONSTRAINT IF EXISTS chk_rsa_beneficiary_kind;
ALTER TABLE revenue_stream_attribution ADD CONSTRAINT chk_rsa_beneficiary_kind CHECK (
       (beneficiary_kind = 'entity'       AND management_entity_id IS NOT NULL)
    OR (beneficiary_kind = 'per_merchant' AND management_entity_id IS NULL));

-- 该表 owner 是 complex_owner(其余表是 complex),按 owner 批量授权的脚本会漏掉它 → 显式重发 GRANT。
DO $$
BEGIN
    IF EXISTS (SELECT 1 FROM pg_roles WHERE rolname = 'complex_app') THEN
        EXECUTE 'GRANT SELECT, INSERT, UPDATE ON revenue_stream_attribution TO complex_app';
    END IF;
END $$;

-- ============================================================================
-- B. settlement_transactions:补主体受益方列(方案 §十六 B-7)
-- ============================================================================
--
-- 现状实测:整张表只有 beneficiary_type 一个归属字段,CHECK 死在 ('merchant','project'),
--   **连主体 id 列都没有** → 新裁决「未核销权益款归娱乐平台公司 / 游乐通票的未核销权益款归游乐公司」
--   在账本层面一行都落不下去。不能把主体塞进 'project' 语义里蒙混:那会让财务在对账时
--   看到「归项目公司」而实际该归娱乐平台公司,是错账不是简写。
--
-- 【与 V344 append-only 触发器的关系】本迁移**只加列、不回填历史行**,
--   DDL 不触发 DML 行触发器,无需 DISABLE TRIGGER(V344:70-79 已就此写明前向兼容口径)。
ALTER TABLE settlement_transactions
    ADD COLUMN IF NOT EXISTS beneficiary_entity_id VARCHAR(64);

COMMENT ON COLUMN settlement_transactions.beneficiary_entity_id IS
    'beneficiary_type=entity 时的收款主体 business_entities.id(如未核销权益款归娱乐平台公司 be_ent_platform、游乐通票未核销款归游乐公司 be_amusement);merchant/project 时为空';

COMMENT ON COLUMN settlement_transactions.beneficiary_type IS
    '收款方类型:merchant=归商户(merchant_id)/project=归项目公司(project_id)/entity=归某经营主体(beneficiary_entity_id,CHECK 强制非空)';

ALTER TABLE settlement_transactions DROP CONSTRAINT IF EXISTS chk_st_beneficiary;
ALTER TABLE settlement_transactions ADD CONSTRAINT chk_st_beneficiary CHECK (
    beneficiary_type IN ('merchant', 'project', 'entity')
    AND (beneficiary_type <> 'entity' OR beneficiary_entity_id IS NOT NULL));
