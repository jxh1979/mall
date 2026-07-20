-- V319 六业态收入归属可配置化:每类业态收入流 → 对应 L3 管理公司/业态主体(business_entities)。
--   口径来源:terminology-canonical-v1.0.md §三(六类业态主体 + 收入归属)。
--   现状缺口:通票分账只有抽象「项目公司留存」,并无把六类收入流指向具体业态主体的映射(硬编码/缺失)。
--   本表把该映射落库且后台可配(治理铁律:业务策略禁硬编码)。
--
--   六业态收入流 → 业态主体(§三):
--     all_pass_ticket    通票                 → 娱乐运营平台
--     table_dining       订台低消/二次消费/额外酒水 → 中央餐饮公司(开关关时归对应酒吧,由酒吧域另管)
--     bar_service_fee    非散台服务费(包厢/卡座)   → 酒吧经营主体(SPV)
--     amusement_ticket   游乐票                → 游乐公司
--     performance_ticket 演出票                → 传媒公司
--     venue_rent         商业租金/场地租金        → 资产公司
--
--   作用域范式照 platform_retention_rules / giveaway_cost_configs:
--     project_id=NULL → 平台默认口径;project_id 非空 → 某项目公司(综合体维度)特例。
--     解析优先级:项目特例 > 平台默认 > 未配置(返 NULL,仅归属留白不改金额)。
--   management_entity_id 松引用 business_entities(id)(本库约定不加硬 FK)。
--   不预置目标主体行:各项目的六个业态主体实体 id 因库而异,留待后台配置(零配置=归属留白)。
CREATE TABLE IF NOT EXISTS revenue_stream_attribution (
    id                   VARCHAR(48) PRIMARY KEY,
    project_id           VARCHAR(64),                         -- NULL=平台默认;非空=项目公司(综合体)特例
    revenue_stream       VARCHAR(32) NOT NULL,                -- 六业态收入流(见上)
    management_entity_id VARCHAR(64) NOT NULL,                -- 收入归属的 L3 管理公司/业态主体 business_entities.id
    status               VARCHAR(16) NOT NULL DEFAULT 'active',
    updated_by           VARCHAR(64),
    updated_at           TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT chk_rsa_stream CHECK (revenue_stream IN (
        'all_pass_ticket', 'table_dining', 'bar_service_fee',
        'amusement_ticket', 'performance_ticket', 'venue_rent')),
    CONSTRAINT chk_rsa_status CHECK (status IN ('active', 'inactive'))
);

-- 同 (项目, 业态流) 至多一条 active 归属(平台默认 project_id=NULL 归一到 __DEFAULT__ 保持唯一)。
CREATE UNIQUE INDEX IF NOT EXISTS uq_rsa_project_stream
    ON revenue_stream_attribution (COALESCE(project_id, '__DEFAULT__'), revenue_stream)
 WHERE status = 'active';

-- 通票结算单标注收入归属主体(§三:通票→娱乐运营平台);解析自 revenue_stream_attribution,未配置=NULL。
ALTER TABLE settlement_bills
    ADD COLUMN IF NOT EXISTS attributed_entity_id VARCHAR(64);

COMMENT ON COLUMN settlement_bills.attributed_entity_id IS
    '通票收入归属的 L3 管理公司/业态主体(娱乐运营平台);解析自 revenue_stream_attribution(all_pass_ticket)';

-- 生产受限运行时权限(V298/V299 教训:新表必须显式 GRANT 给 complex_app,否则生产 500)。
DO $$
BEGIN
    IF EXISTS (SELECT 1 FROM pg_roles WHERE rolname = 'complex_app') THEN
        EXECUTE 'GRANT SELECT, INSERT, UPDATE ON revenue_stream_attribution TO complex_app';
    END IF;
END $$;
