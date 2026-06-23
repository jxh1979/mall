-- V149 日结锁定治理表 finance_daily_closes 纳入 append-only（delete_only）防篡改（A2-02 / P1-9）。
-- 背景：所有资金台账（ar_ap_items[V145]/ar_ap_payments[V140]、deposit_ledger、cam_trueup_* 等）均已纳入
--   append-only，但「日结锁」本身这张治理表此前无任何 DB 层防护——锁记录可被物理 DELETE 抹除，从而绕过
--   「封账后不得直接改影响金额、须上级审批补录」的铁律，且事后无法从 DB 约束层证明锁从未被动过。
-- 口径：finance_daily_closes 含合法状态 upsert（saveDailyClose ON CONFLICT (id) DO UPDATE status），故纳入
--   delete_only（仅禁物理 DELETE 以永久留痕锁的存在，保留合法 UPDATE 供状态回写），与 V140/V145 对
--   delete_only 表口径一致；复用 V140 既建的 complex_block_mutation() 触发器函数。
-- 残留说明：经 UPDATE 翻转锁状态（locked→其它）属应用层职责——finance 域无 reopen 路径（已 grep 确认），
--   补录一律走 ar_ap_adjustments 审批链；DB 层防删确保锁记录不可被悄悄抹除，是纵深防御的关键一环。

DO $$
BEGIN
    IF to_regclass('finance_daily_closes') IS NOT NULL THEN
        EXECUTE 'DROP TRIGGER IF EXISTS trg_append_only_d ON finance_daily_closes';
        EXECUTE 'CREATE TRIGGER trg_append_only_d BEFORE DELETE ON finance_daily_closes '
             || 'FOR EACH ROW EXECUTE FUNCTION complex_block_mutation()';
    END IF;
END $$;

-- 生产受限运行时 role 纵深：与 V140/V145 对 delete_only 表口径一致，REVOKE DELETE
-- （role 不存在则跳过，由运维预置 complex_app 时生效）。
DO $$
BEGIN
    IF to_regclass('finance_daily_closes') IS NOT NULL
       AND EXISTS (SELECT 1 FROM pg_roles WHERE rolname = 'complex_app') THEN
        EXECUTE 'REVOKE DELETE ON finance_daily_closes FROM complex_app';
    END IF;
END $$;
