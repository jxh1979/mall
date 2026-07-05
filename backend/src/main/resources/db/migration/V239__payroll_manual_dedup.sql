-- 薪资资金安全②【P0】手工提成/奖惩无去重(fix/payroll-safety)。
--   缺陷:manual 提成与所有 adjustment 无业务唯一约束(只 service_order 来源有部分唯一索引);
--         同 user/month/amount/note 不同幂等键提交两次 → 翻倍套现。
--   修:manual 提成 + 奖惩各加业务级唯一约束。note/reason 走 COALESCE 归一,避免 NULL 破坏去重。
--       仅约束 manual 来源提成;service_order 等系统来源仍由 V215 部分唯一索引与幂等键管。

-- 先清历史重复(保留最早一条,与 V215 同手法),再建唯一索引。
DELETE FROM staff_commission_ledger a
 USING staff_commission_ledger b
 WHERE a.source_type = 'manual' AND b.source_type = 'manual'
   AND a.user_id = b.user_id AND a.month = b.month
   AND a.amount_cent = b.amount_cent
   AND COALESCE(a.note, '') = COALESCE(b.note, '')
   AND a.ctid > b.ctid;

CREATE UNIQUE INDEX IF NOT EXISTS uq_commission_manual_business
    ON staff_commission_ledger (user_id, month, amount_cent, COALESCE(note, ''))
    WHERE source_type = 'manual';

DELETE FROM staff_payroll_adjustments a
 USING staff_payroll_adjustments b
 WHERE a.user_id = b.user_id AND a.month = b.month
   AND a.adj_type = b.adj_type AND a.amount_cent = b.amount_cent
   AND COALESCE(a.reason, '') = COALESCE(b.reason, '')
   AND a.ctid > b.ctid;

CREATE UNIQUE INDEX IF NOT EXISTS uq_adjustment_business
    ON staff_payroll_adjustments (user_id, month, adj_type, amount_cent, COALESCE(reason, ''));
