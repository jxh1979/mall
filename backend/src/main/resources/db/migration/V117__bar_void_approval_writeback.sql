-- ④ P2：酒吧 void 审批回写流水 status。
--
-- 背景（审计）：退菜退酒 void 是高危需审批动作（permissions.bar.order_item.void requires_approval=TRUE），
-- 但 bar_order_item_voids 流水的 status 此前恒写 'applied'，approval_required 标志无任何后续状态机消费，
-- 即“审批结果未落到 void 流水记录上”。
--
-- 本迁移：让 void 流水 status 承载审批生命周期 pending → approved/rejected，并补审批留痕列。
--   - approval_required=TRUE 的 void 落库为 'pending'（待审批）；approval_required=FALSE 落 'applied'（无需审批，直接生效）。
--   - 审批决策端点把 'pending' 回写为 'approved'（通过）或 'rejected'（驳回），并记审批人/时点/理由。
--   - 向后兼容：旧约定 status='applied' 仍合法（沽清/不需审批路径与历史数据）；不破坏既有同步生效语义。

-- 1) 审批留痕列（决策回写时填充；'applied'/'pending' 态为空）。
ALTER TABLE bar_order_item_voids
    ADD COLUMN IF NOT EXISTS reviewed_by_user_id VARCHAR(64),
    ADD COLUMN IF NOT EXISTS reviewed_at TIMESTAMPTZ,
    ADD COLUMN IF NOT EXISTS review_reason VARCHAR(500);

-- 2) status 取值约束：放开为审批生命周期 + 兼容历史 'applied'。
DO $$
BEGIN
    IF EXISTS (
        SELECT 1 FROM information_schema.table_constraints
         WHERE constraint_name = 'chk_bar_order_item_voids_status'
    ) THEN
        ALTER TABLE bar_order_item_voids DROP CONSTRAINT chk_bar_order_item_voids_status;
    END IF;
    ALTER TABLE bar_order_item_voids
        ADD CONSTRAINT chk_bar_order_item_voids_status CHECK (
            status IN ('pending', 'approved', 'rejected', 'applied')
        );
END $$;

-- 3) 审批一致性：approved/rejected 必须有审批人与时点（pending/applied 不得携带审批留痕）。
DO $$
BEGIN
    IF EXISTS (
        SELECT 1 FROM information_schema.table_constraints
         WHERE constraint_name = 'chk_bar_order_item_voids_review'
    ) THEN
        ALTER TABLE bar_order_item_voids DROP CONSTRAINT chk_bar_order_item_voids_review;
    END IF;
    ALTER TABLE bar_order_item_voids
        ADD CONSTRAINT chk_bar_order_item_voids_review CHECK (
            (status IN ('approved', 'rejected')
                AND reviewed_by_user_id IS NOT NULL AND reviewed_at IS NOT NULL)
            OR (status IN ('pending', 'applied')
                AND reviewed_by_user_id IS NULL AND reviewed_at IS NULL AND review_reason IS NULL)
        );
END $$;
