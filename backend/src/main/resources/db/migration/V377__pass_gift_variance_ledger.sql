-- V377 通票分账瀑布重建 S7:赠付差额调账留痕 + 兑付成本按业态分记。
--
-- 【业务依据】(最终解释权=三份业务 HTML,逐字对齐)
--   business-system-map-for-operations §「赠饮/赠送权益结算」(:962):
--     「对商户的兑付由娱乐平台公司承担,不进入酒吧可分配池,也不计入酒吧的销售提成;
--      成本按提供方的商户业态(酒吧/餐厅/零售)分开记账;
--      预扣与核销实付的差额留在娱乐平台公司,按可配规则调账留痕。」
--   预扣(G)已由 S6(V372/V373)逐笔落 settlement_transactions.gift_cost_cent;
--   兑付实付(A)已由 bar 域逐笔挂 ar_ap_items(source_type='pass_drink_settlement',冲正对冲);
--   差额 V = G − A 至今【没有任何调账留痕】——本迁移补上消费载体。
--
-- 【本迁移做两件事】
--   ① 新建差额调账台账 pass_gift_cost_variance_ledger:月结(settleMonth)时按「项目 × 规则
--      (pass_gift_cost_configs)」每期结出一行(预扣合计/实付合计/差额/按哪条规则处理);
--      append-only(V344 触发器族),月结重算走「冲正取反 + 重述」两行追加,绝不改历史行;
--      差额恒等式 V = G − A 直接写进 DB CHECK——代码算错插不进去(K12 的数据库层锁)。
--   ② settlement_bill_format_shares 加兑付实付列 gift_actual_paid_cent:兑付成本按【提供方
--      商户业态】(merchants.merchant_type 五值,V348 封口)分开记账,财务在结算审阅页直接看
--      「这个月娱乐平台公司给酒吧/餐厅/零售各兑付了多少」(K14:Σ各业态实付 ≡ 台账实付合计)。
--
-- 【三种差额处理规则】(语义按 V373 快照定义,月结时按解析到的生效规则处理并快照进行)
--   platform_absorb = 差额留娱乐平台公司仅留痕(默认,HTML 口径);
--   carry_forward   = 结转下期(carried_forward_cent 滚入下期同「项目×规则」的 carried_in_cent);
--   true_up_pool    = 回补当期可分配池(pool_true_up_cent 加回当期池,影响当月酒吧分账)。
--
-- 【零金额影响】默认种子 platform_absorb + 差额为零时不落行 → 既有结算单逐分不变;
--   开发环境数据最后全删,不做存量回填。

-- ==========================================================================
-- ① 差额调账台账(append-only)
-- ==========================================================================

CREATE TABLE IF NOT EXISTS pass_gift_cost_variance_ledger (
    id                    VARCHAR(64) PRIMARY KEY,
    bill_id               VARCHAR(64) NOT NULL,        -- 松引用 settlement_bills(id)(单可幂等重建,不设 FK 级联)
    project_id            VARCHAR(64),                 -- NULL=平台默认口径(与结算单同义)
    period_start          DATE        NOT NULL,
    period_end            DATE        NOT NULL,
    config_id             VARCHAR(64) NOT NULL,        -- pass_gift_cost_configs.id(「项目×规则」的规则锚点)
    variance_handling     VARCHAR(24) NOT NULL,        -- 本行按哪条规则处理(月结时快照,后改配置不追溯)
    accrued_cost_cent     BIGINT      NOT NULL,        -- G:当期预扣合计(带符号,含冲正)
    actual_paid_cent      BIGINT      NOT NULL,        -- A:当期兑付实付合计(带符号,含对冲)
    variance_cent         BIGINT      NOT NULL,        -- V = G − A(带符号)
    carried_in_cent       BIGINT      NOT NULL DEFAULT 0,  -- 上期 carry_forward 滚入本期的部分
    carried_forward_cent  BIGINT      NOT NULL DEFAULT 0,  -- carry_forward 规则:结转下期的金额(= V + carried_in)
    pool_true_up_cent     BIGINT      NOT NULL DEFAULT 0,  -- true_up_pool 规则:回补当期池的金额(= V + carried_in)
    beneficiary_entity_id VARCHAR(64),                 -- 娱乐平台公司主体快照(pass_gift_cost 归属流,与结算单同源)
    reversal_of           VARCHAR(64),                 -- 非空=本行为该行的冲正行(月结重算取反);更正只追加不回改
    created_at            TIMESTAMPTZ NOT NULL DEFAULT now(),
    -- K12 差额恒等式的数据库层锁:任何一行(含冲正行,取反后仍成立)都必须满足 V = G − A。
    CONSTRAINT chk_pgcv_identity CHECK (variance_cent = accrued_cost_cent - actual_paid_cent),
    CONSTRAINT chk_pgcv_handling CHECK (variance_handling IN ('platform_absorb', 'carry_forward', 'true_up_pool')),
    -- 三种规则互斥:结转与回补池不可同时发生(platform_absorb 两者皆 0)。
    CONSTRAINT chk_pgcv_exclusive CHECK (carried_forward_cent = 0 OR pool_true_up_cent = 0)
);

-- 单据视角(结算审阅页按单聚合)与跨期 carry 追溯(同项目×规则按期扫)各一条索引。
CREATE INDEX IF NOT EXISTS idx_pgcv_bill ON pass_gift_cost_variance_ledger (bill_id);
CREATE INDEX IF NOT EXISTS idx_pgcv_carry
    ON pass_gift_cost_variance_ledger (config_id, period_start);

COMMENT ON TABLE pass_gift_cost_variance_ledger IS
    'S7 赠付差额调账台账(HTML:962「预扣与核销实付的差额留在娱乐平台公司,按可配规则调账留痕」):'
    '月结按「项目×规则」每期一行,预扣 G=settlement_transactions.gift_cost_cent 线性求和、'
    '实付 A=ar_ap_items(pass_drink_settlement) 对冲净额;append-only,月结重算冲正取反追加,不改历史行';
COMMENT ON COLUMN pass_gift_cost_variance_ledger.variance_handling IS
    'V373 快照三值:platform_absorb=差额留娱乐平台公司仅留痕(默认)/carry_forward=结转下期/true_up_pool=回补当期池';
COMMENT ON COLUMN pass_gift_cost_variance_ledger.carried_in_cent IS
    '此前各期 carry_forward 结转、尚未被处理的净额(Σ往期 carried_forward − Σ往期 carried_in),本期一并按本期规则处理';
COMMENT ON COLUMN pass_gift_cost_variance_ledger.reversal_of IS
    '非空=本行为该 id 行的冲正行(全额取反):settleMonth 幂等重算时先冲掉旧结论再追加新结论,历史留痕不灭';

-- append-only 硬强制(V344 范式:V140 complex_block_mutation 触发器 + 运行时角色 REVOKE 双层)。
DO $$
BEGIN
    IF to_regclass('pass_gift_cost_variance_ledger') IS NOT NULL THEN
        EXECUTE 'DROP TRIGGER IF EXISTS trg_append_only_ud ON pass_gift_cost_variance_ledger';
        EXECUTE 'CREATE TRIGGER trg_append_only_ud BEFORE UPDATE OR DELETE ON pass_gift_cost_variance_ledger '
             || 'FOR EACH ROW EXECUTE FUNCTION complex_block_mutation()';
    END IF;
END $$;

-- 生产受限运行时权限(V296/V298/V338 教训:新表必须显式 GRANT,否则运行时静默 permission denied 500);
-- 台账只许追加与读取,UPDATE/DELETE 连授都不授(与触发器双层)。
DO $$
BEGIN
    IF EXISTS (SELECT 1 FROM pg_roles WHERE rolname = 'complex_app') THEN
        EXECUTE 'GRANT SELECT, INSERT ON pass_gift_cost_variance_ledger TO complex_app';
        EXECUTE 'REVOKE UPDATE, DELETE ON pass_gift_cost_variance_ledger FROM complex_app';
    END IF;
END $$;

-- ==========================================================================
-- ② 兑付成本按业态分记:业态份额行加实付列
-- ==========================================================================

-- 预扣侧 G 已按业态分记(V372 gift_cost_cent 跟核销走);本列补【兑付实付 A】侧:
-- 业态 = 提供该权益的商户(ar_ap 对手方)的 merchants.merchant_type,与 S11/S4/S6 的
-- 业态归堆同一取法(月结时联 merchants 主数据,同一批行两侧口径恒一致)。
-- 带符号:冲正月对冲多于新挂账时本列为负(向该业态商户净追回)。
-- 本列纯留痕,不进净份额公式(兑付由娱乐平台公司承担,不动各业态分账,HTML:962)。
ALTER TABLE settlement_bill_format_shares
    ADD COLUMN IF NOT EXISTS gift_actual_paid_cent BIGINT NOT NULL DEFAULT 0;

COMMENT ON COLUMN settlement_bill_format_shares.gift_actual_paid_cent IS
    'S7 本业态当月赠送权益兑付实付合计(A,娱乐平台公司按内部结算价付给该业态提供方商户的净额,'
    '含冲正对冲;业态=提供方商户 merchant_type)。纯留痕不进净份额;K14:Σ各业态本列 ≡ 差额台账实付合计';
