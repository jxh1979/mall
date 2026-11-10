-- 文件版本：V0.01
-- V614：第十八批 AB 段第 190 条（转台次数上限、超限主管审批、撤销转台），按步骤就地追加：第 1 节 190-R1 转台次数上限；第 2 节 190-R2 超限转台申请；第 3 节 190-R3 撤销转台留痕类型。
-- 依据：BSM:1079「同一会话的转台次数不得超过后台配置的上限（业务参数，按酒吧可配，出厂默认 10 次，须同时配置上下限），达到上限后须主管审批才能继续转台」；
--       决策台账 DEC-R6-19「循环转台＝同一会话转台次数上限（业务参数，按酒吧可配，出厂默认 10 次，须同时配置上下限）」。

-- 1) 190-R1 转台次数上限：按酒吧可配。酒吧＝merchant_type='bar' 的商户（V267 裁定），参数落 merchants 列；
--    范式同 V521 待清台提醒时限（值与上下限同配）：未配置＝三列皆空，按出厂 10 次；
--    配置时值、下限、上限三者同填且 0≤下限≤值≤上限（不另设硬上下限）。
ALTER TABLE merchants ADD COLUMN IF NOT EXISTS table_transfer_limit INTEGER;
ALTER TABLE merchants ADD COLUMN IF NOT EXISTS table_transfer_limit_min INTEGER;
ALTER TABLE merchants ADD COLUMN IF NOT EXISTS table_transfer_limit_max INTEGER;
ALTER TABLE merchants DROP CONSTRAINT IF EXISTS chk_merchants_table_transfer_limit;
ALTER TABLE merchants ADD CONSTRAINT chk_merchants_table_transfer_limit CHECK (
    (table_transfer_limit IS NULL AND table_transfer_limit_min IS NULL AND table_transfer_limit_max IS NULL)
    OR (table_transfer_limit IS NOT NULL AND table_transfer_limit_min IS NOT NULL AND table_transfer_limit_max IS NOT NULL
        AND table_transfer_limit_min >= 0
        AND table_transfer_limit_min <= table_transfer_limit
        AND table_transfer_limit <= table_transfer_limit_max)
);
COMMENT ON COLUMN merchants.table_transfer_limit IS '同一会话转台次数上限（BSM:1079；未配置＝空，按出厂 10 次；与上下限同配；达上限须主管审批才能继续转台）';
COMMENT ON COLUMN merchants.table_transfer_limit_min IS '转台次数上限已配置下限（次，与值、上限同配）';
COMMENT ON COLUMN merchants.table_transfer_limit_max IS '转台次数上限已配置上限（次，与值、下限同配）';

-- 2) 190-R2 超限转台申请：同一会话已转台次数达到本酒吧上限时，转台须主管审批（BSM:1079「达到上限后须主管审批才能继续转台」；
--    DEC-R6-19「达上限须主管审批」）。逐次审批、一次用一张：pending→approved/rejected，批准的申请只用于一次转到申请里那张目标台的转台（→used）；
--    同一会话同时只有一张待审批。申请在转台事务锁住会话行之后由独立事务写入（转台随后 409 回滚，申请与复核单仍在），
--    故 session_id、target_table_id 不设外键（外键检查要等转台事务持有的行锁）。研发期无存量（UD-07）。
CREATE TABLE IF NOT EXISTS bar_table_transfer_limit_approvals (
    id VARCHAR(64) PRIMARY KEY,
    session_id VARCHAR(64) NOT NULL,
    bar_id VARCHAR(64) NOT NULL,
    merchant_id VARCHAR(64),
    target_table_id VARCHAR(64) NOT NULL,
    transfer_count INTEGER NOT NULL,
    limit_count INTEGER NOT NULL,
    reason TEXT NOT NULL,
    requested_by VARCHAR(64) NOT NULL,
    review_id VARCHAR(64) NOT NULL,
    status VARCHAR(16) NOT NULL DEFAULT 'pending',
    adjustment_id VARCHAR(64),
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    decided_at TIMESTAMPTZ,
    used_at TIMESTAMPTZ,
    CONSTRAINT chk_bar_table_transfer_limit_approvals_status CHECK (status IN ('pending', 'approved', 'rejected', 'used'))
);
CREATE UNIQUE INDEX IF NOT EXISTS uq_bar_table_transfer_limit_approvals_pending
    ON bar_table_transfer_limit_approvals(session_id) WHERE status = 'pending';
COMMENT ON TABLE bar_table_transfer_limit_approvals IS '超限转台申请（BSM:1079 达到转台次数上限后须主管审批才能继续转台；批准一次只用于一次转到申请里那张目标台的转台）';

DO $$
BEGIN
    IF EXISTS (SELECT 1 FROM pg_roles WHERE rolname = 'complex_app') THEN
        EXECUTE 'GRANT SELECT, INSERT, UPDATE ON bar_table_transfer_limit_approvals TO complex_app';
    END IF;
END $$;

-- 3) 190-R3 撤销转台：台位会话调整类型闭集扩 transfer_undo（BSM:1079「每次拆单、合单、转台、并台、拆台和撤销转台均须留痕」；
--    决策台账 DEC-R6-19「撤销转台＝按原路回退到转台前的台位与进度并留痕」）。写法同 V521。
ALTER TABLE bar_table_session_adjustments DROP CONSTRAINT IF EXISTS chk_bar_table_adjustment_type;
ALTER TABLE bar_table_session_adjustments ADD CONSTRAINT chk_bar_table_adjustment_type CHECK (
    adjustment_type IN ('transfer', 'merge', 'renew', 'split', 'transfer_undo')
);
