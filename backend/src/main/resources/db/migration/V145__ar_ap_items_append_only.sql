-- V145 资金权威台账 ar_ap_items 纳入 append-only（delete_only）保护（F-OPS-01）。
-- 背景：V140 已保护 ar_ap_payments(immutable) / ar_ap_item_sources(delete_only)，但运行期权威资金台账主表
--   ar_ap_items（FinanceService/JdbcFinanceRepository 实际读写 amount_cent/paid/unpaid/status）被遗漏，
--   可被任意 UPDATE/DELETE 篡改金额或经 ON DELETE CASCADE 抹除——资金台账可篡改 P0。
-- 口径：ar_ap_items 是幂等 upsert（markPaymentSucceeded/退款等合法 UPDATE paid/unpaid/status），故纳入 delete_only
--   （仅禁物理 DELETE 以永久留痕，保留合法 UPDATE 供状态/金额回写；金额修正走冲正分录而非删行），
--   与 V140 对 ar_ap_item_sources 等 delete_only 表口径一致。复用 V140 既建的 complex_block_mutation() 触发器函数。

DO $$
BEGIN
    IF to_regclass('ar_ap_items') IS NOT NULL THEN
        EXECUTE 'DROP TRIGGER IF EXISTS trg_append_only_d ON ar_ap_items';
        EXECUTE 'CREATE TRIGGER trg_append_only_d BEFORE DELETE ON ar_ap_items '
             || 'FOR EACH ROW EXECUTE FUNCTION complex_block_mutation()';
    END IF;
END $$;

-- 生产受限运行时 role 纵深：与 V140 对 delete_only 表口径一致，REVOKE DELETE（role 不存在则跳过，由运维预置时生效）。
DO $$
BEGIN
    IF to_regclass('ar_ap_items') IS NOT NULL
       AND EXISTS (SELECT 1 FROM pg_roles WHERE rolname = 'complex_app') THEN
        EXECUTE 'REVOKE DELETE ON ar_ap_items FROM complex_app';
    END IF;
END $$;
