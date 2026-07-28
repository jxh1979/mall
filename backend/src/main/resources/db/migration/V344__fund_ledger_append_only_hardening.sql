-- V344 P0-B 资金台账 append-only 破防三处修复（v7 综合审计「批 P0 止血」第 3 项）。
--
-- 【三处破口（实读结论）】
--  ① ar_ap_items 锁定后补录改原行金额：FinanceService.decideAdjustment 审批通过后调
--     JdbcFinanceRepository.updateArApItemLedger 直接改原行（原行金额被覆盖、历史值不可考），
--     违反「资金台账只追加、更正走红字冲销」的既定口径（V145/V297 注释均已写死该口径）。
--     更严重：updateArApItemLedger 的 SET 列表根本不含 amount_cent，只写 paid/unpaid/status，
--     实跑会落出 paid+unpaid <> amount 的行 → 触发 V159 chk_ar_ap_items_amount_balance 直接失败，
--     该补录审批链在真库从未跑通（单测用内存假仓，假仓把 amount 一起改了，故绿灯掩盖）。
--     本迁移建红冲底座：原行永不动，追加「严格取反的冲正行」+「重述行」两条新行。
--  ② settlement_transactions（通票分账最高价值账本）自建表起就没有 append-only 触发器：
--     V323 已把它重构成「每业务事件一行不可变资金事实」，应用侧只 INSERT ... ON CONFLICT DO NOTHING、
--     永不 UPDATE，但 DB 层零防护——任何一条 UPDATE/DELETE 都能悄悄改掉/抹掉分账事实。
--     本迁移补 V140 触发器族（immutable：禁 UPDATE + DELETE）+ 运行时角色 REVOKE。
--  ③ customer_points_ledger.confirmed_amount（会员真正入账的积分份额）被就地 UPDATE 覆盖：
--     LoyaltyService.confirmPoints 每次核销/冲正把 earn 行的 confirmed_amount 改写成新的 target，
--     旧值不留痕（V140 注释即已挂账「理想应改追加冲正分录，另议」）。
--     本迁移把「已确认份额」改为追加式 confirm 分录 + 冻结 earn 行事实列。
--
-- 【范式对齐】沿用 V140 建的 complex_block_mutation() 触发器函数、V149/V278 的
--   「to_regclass 存在判定 + DROP TRIGGER IF EXISTS 再建 + role 存在才 REVOKE」写法，异常码统一 23514、
--   报文统一以 COMPLEX_APPEND_ONLY_VIOLATION 开头（既有门禁测试按该前缀断言）。

-- ============================================================================
-- ① ar_ap_items：补录红冲底座（原行不动，追加冲正行 + 重述行）
-- ============================================================================

-- 1.1 溯源列：冲正行与重述行都回指原行，并记录产生它们的补录工单。
--     行角色可由金额符号判读：origin_item_id 非空且 amount_cent<0 = 冲正行；非空且 >0 = 重述行；为空 = 原始行。
ALTER TABLE ar_ap_items ADD COLUMN IF NOT EXISTS origin_item_id VARCHAR(128);
ALTER TABLE ar_ap_items ADD COLUMN IF NOT EXISTS adjustment_id  VARCHAR(128);

DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'ar_ap_items_origin_item_id_fkey') THEN
        ALTER TABLE ar_ap_items
            ADD CONSTRAINT ar_ap_items_origin_item_id_fkey
            FOREIGN KEY (origin_item_id) REFERENCES ar_ap_items(id);
    END IF;
END $$;

CREATE INDEX IF NOT EXISTS idx_ar_ap_items_origin
    ON ar_ap_items (origin_item_id) WHERE origin_item_id IS NOT NULL;

-- 1.2 金额符号约束改为「符号自洽」：原始行/重述行三个金额列全部 >0 / >=0（与改造前逐字等价）；
--     冲正行（必须挂 origin_item_id）三个金额列全部取反、恒为 <=0 且总额 <0。
--     账平恒等 chk_ar_ap_items_amount_balance（paid+unpaid=amount）不动——正负两侧都成立，
--     故「原行 + 冲正行 + 重述行」三行相加天然守恒，无舍入漂移。
ALTER TABLE ar_ap_items DROP CONSTRAINT IF EXISTS chk_ar_ap_items_paid_non_negative;
ALTER TABLE ar_ap_items DROP CONSTRAINT IF EXISTS chk_ar_ap_items_unpaid_non_negative;
ALTER TABLE ar_ap_items DROP CONSTRAINT IF EXISTS chk_ar_ap_items_amount_positive;
ALTER TABLE ar_ap_items ADD CONSTRAINT chk_ar_ap_items_amount_positive CHECK (
    (amount_cent > 0 AND paid_amount_cent >= 0 AND unpaid_amount_cent >= 0)
    OR (origin_item_id IS NOT NULL
        AND amount_cent < 0 AND paid_amount_cent <= 0 AND unpaid_amount_cent <= 0)
);

-- 1.3 「一个来源单据一条台账」唯一约束保持不变，但排除补录派生行——
--     冲正行/重述行刻意沿用原行的 source_type/source_id（下游按来源聚合才能自动净额正确），
--     它们不是新的来源单据，幂等由确定性主键（arap_rev_/arap_res_ + 工单 id）+ 审批状态机承担。
DROP INDEX IF EXISTS uq_ar_ap_items_direction_source;
CREATE UNIQUE INDEX uq_ar_ap_items_direction_source
    ON ar_ap_items (direction, source_type, source_id)
 WHERE source_id IS NOT NULL AND origin_item_id IS NULL;

-- ============================================================================
-- ② settlement_transactions：补 append-only 触发器族（V140 immutable 口径）
-- ============================================================================
--
-- 【前向兼容（关键）】本触发器是整行级拦截（BEFORE UPDATE OR DELETE FOR EACH ROW），
--   函数体不引用任何具体列名，也没有「允许列白名单」——因此后续「通票分账瀑布重建」等工作
--   继续往该表 ADD COLUMN 完全不受影响（DDL 不触发 DML 行触发器；PG 11+ 带默认值加列还是纯元数据操作）。
--   唯一需要留意的是「加列后要回填历史行」的迁移（V323 曾这么做过）：
--     那类回填 UPDATE 会被本触发器挡下，迁移内必须显式摘挂：
--       ALTER TABLE settlement_transactions DISABLE TRIGGER trg_append_only_ud;
--       UPDATE settlement_transactions SET 新列 = ... WHERE 新列 IS NULL;
--       ALTER TABLE settlement_transactions ENABLE  TRIGGER trg_append_only_ud;
--   DISABLE TRIGGER 需要表 owner 权限，迁移以 owner 身份跑得通；而生产运行时角色 complex_app
--   既非 owner（改不了触发器）又被下面 REVOKE 掉 UPDATE/DELETE，两层都堵死，回填口子不构成运行期敞口。
DO $$
BEGIN
    IF to_regclass('settlement_transactions') IS NOT NULL THEN
        EXECUTE 'DROP TRIGGER IF EXISTS trg_append_only_ud ON settlement_transactions';
        EXECUTE 'CREATE TRIGGER trg_append_only_ud BEFORE UPDATE OR DELETE ON settlement_transactions '
             || 'FOR EACH ROW EXECUTE FUNCTION complex_block_mutation()';
    END IF;
END $$;

-- 生产受限运行时 role 纵深：与 V140 immutable 表口径一致，REVOKE UPDATE+DELETE
-- （role 不存在则跳过，由运维预置 complex_app 时生效）。
DO $$
BEGIN
    IF to_regclass('settlement_transactions') IS NOT NULL
       AND EXISTS (SELECT 1 FROM pg_roles WHERE rolname = 'complex_app') THEN
        EXECUTE 'REVOKE UPDATE, DELETE ON settlement_transactions FROM complex_app';
    END IF;
END $$;

-- ============================================================================
-- ③ customer_points_ledger：已确认份额改追加 + 冻结事实列
-- ============================================================================

-- 3.1 允许同一来源单据反复追加 confirm 分录（部分核销 1/3 → 2/3 → 3/3 各落一行；冲正落负行）。
--     其余 change_type（earn/deduct/expire 等）的「一单一行」唯一性照旧。
DROP INDEX IF EXISTS uq_customer_points_ledger_source_doc;
CREATE UNIQUE INDEX uq_customer_points_ledger_source_doc
    ON customer_points_ledger (source_doc_type, source_doc_id, change_type)
 WHERE source_doc_type IS NOT NULL AND source_doc_id IS NOT NULL AND change_type <> 'confirm';

-- 3.2 存量补建：历史 earn 行的 confirmed_amount 已就地累加成现值，改追加后「已确认份额」
--     的权威口径变成 Σ(confirm 分录)。若不补建，存量单的派生值会突然变 0 →
--     再来一次核销事件会把整份积分二次入账（重复发分）。故每条 confirmed_amount>0 的 earn 行
--     补一条金额严格等于该值的 confirm 分录，令「派生值 == 原就地值」逐单相等、零行为改变。
INSERT INTO customer_points_ledger (
    id, customer_id, points_delta, balance_after, source_type, source_id, description,
    created_at, expire_at, remaining_points, expired, change_type, status,
    source_doc_type, source_doc_id, confirmed_amount)
SELECT 'lcbf_' || md5(e.id),
       e.customer_id,
       e.confirmed_amount,
       e.balance_after,
       'confirm',
       e.id,
       '历史已确认份额补建（V344 台账改追加，金额与原 earn 行 confirmed_amount 严格相等）',
       e.created_at,
       NULL,
       NULL,
       FALSE,
       'confirm',
       e.status,
       e.source_doc_type,
       e.source_doc_id,
       0
  FROM customer_points_ledger e
 WHERE e.change_type = 'earn'
   AND e.confirmed_amount > 0
   AND NOT EXISTS (SELECT 1 FROM customer_points_ledger c
                    WHERE c.change_type = 'confirm' AND c.source_id = e.id)
ON CONFLICT (id) DO NOTHING;

-- 3.3 冻结事实列：earn/confirm/deduct/expire 各行的「金额与归属」一经落表不得改写。
--     刻意采用冻结清单（deny-list）而非允许清单：后续给该表加的新列默认可改，
--     不会因为本触发器写死列名把后面的迭代堵死；而今天所有承载资金/积分事实的列全在清单内。
--     放行列（生命周期/FIFO 批次消耗的运行态，与 V145 对 ar_ap_items.paid/unpaid 的口径同源）：
--       remaining_points（批次可过期余额递减）、expired（批次过期标记）、status（pending→confirmed 等）、
--       description（说明文案）。
CREATE OR REPLACE FUNCTION complex_points_ledger_freeze_facts() RETURNS trigger
LANGUAGE plpgsql AS $$
BEGIN
    IF NEW.customer_id      IS DISTINCT FROM OLD.customer_id
       OR NEW.points_delta     IS DISTINCT FROM OLD.points_delta
       OR NEW.balance_after    IS DISTINCT FROM OLD.balance_after
       OR NEW.change_type      IS DISTINCT FROM OLD.change_type
       OR NEW.source_type      IS DISTINCT FROM OLD.source_type
       OR NEW.source_id        IS DISTINCT FROM OLD.source_id
       OR NEW.source_doc_type  IS DISTINCT FROM OLD.source_doc_type
       OR NEW.source_doc_id    IS DISTINCT FROM OLD.source_doc_id
       OR NEW.confirmed_amount IS DISTINCT FROM OLD.confirmed_amount
       OR NEW.expire_at        IS DISTINCT FROM OLD.expire_at
       OR NEW.created_at       IS DISTINCT FROM OLD.created_at
    THEN
        RAISE EXCEPTION 'COMPLEX_APPEND_ONLY_VIOLATION: UPDATE of a frozen fact column on table % is forbidden (points ledger is append-only; append a confirm entry instead)',
            TG_TABLE_NAME
            USING ERRCODE = '23514';
    END IF;
    RETURN NEW;
END;
$$;

DO $$
BEGIN
    IF to_regclass('customer_points_ledger') IS NOT NULL THEN
        EXECUTE 'DROP TRIGGER IF EXISTS trg_points_ledger_freeze_facts ON customer_points_ledger';
        EXECUTE 'CREATE TRIGGER trg_points_ledger_freeze_facts BEFORE UPDATE ON customer_points_ledger '
             || 'FOR EACH ROW EXECUTE FUNCTION complex_points_ledger_freeze_facts()';
    END IF;
END $$;
