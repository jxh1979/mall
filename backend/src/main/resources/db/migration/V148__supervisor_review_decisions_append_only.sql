-- V148 主管复核决策 supervisor_review_decisions 纳入 append-only（delete_only）保护（F-CROSS-06 / P1 留痕防篡改）。
-- 背景：supervisor_review_decisions（V057 起）记录准入闸/复核的 block/approve/reject 治理决策（合规留痕），
--   但 V140 append-only 清单遗漏该表，可被物理 DELETE 抹除复核留痕——治理留痕可篡改缺口。
-- 口径：该表有合法 UPDATE（评估 pending → 复核 approved/rejected：review_status/reviewer_level/decision/reason 回写），
--   故纳入 delete_only（仅禁物理 DELETE 以永久留痕，保留合法 UPDATE 供状态回写），与 V140 对 delete_only 表口径一致。
--   全代码库零 DELETE supervisor_review_decisions（核验通过）。复用 V140 既建的 complex_block_mutation() 触发器函数。

DO $$
BEGIN
    IF to_regclass('supervisor_review_decisions') IS NOT NULL THEN
        EXECUTE 'DROP TRIGGER IF EXISTS trg_append_only_d ON supervisor_review_decisions';
        EXECUTE 'CREATE TRIGGER trg_append_only_d BEFORE DELETE ON supervisor_review_decisions '
             || 'FOR EACH ROW EXECUTE FUNCTION complex_block_mutation()';
    END IF;
END $$;

-- 生产受限运行时 role 纵深：与 V140/V145 对 delete_only 表口径一致，REVOKE DELETE（role 不存在则跳过）。
DO $$
BEGIN
    IF to_regclass('supervisor_review_decisions') IS NOT NULL
       AND EXISTS (SELECT 1 FROM pg_roles WHERE rolname = 'complex_app') THEN
        EXECUTE 'REVOKE DELETE ON supervisor_review_decisions FROM complex_app';
    END IF;
END $$;
