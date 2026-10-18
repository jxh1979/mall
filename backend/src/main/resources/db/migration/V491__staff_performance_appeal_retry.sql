-- 文件版本：V0.01
-- 驳回后可按既有申诉规则重提；待审或已撤销的同一扣分不得重复申诉。
ALTER TABLE staff_performance_score_adjustments
    DROP CONSTRAINT staff_performance_score_adjustments_reversal_of_key;

CREATE UNIQUE INDEX uq_staff_performance_active_appeal
    ON staff_performance_score_adjustments (reversal_of)
    WHERE reversal_of IS NOT NULL AND status IN ('pending_review', 'approved');
