-- V447 CG-08b 全园故障补偿通票（ENUM「全园故障与补偿通票」）：
--   「只有整个游乐园发生故障并影响整体履约时，才可以向受影响客人发放补偿通票；补偿对象包括故障发生时已经完成入场核销的客人，
--    也包括尚未核销但对应门票或权益仍在有效期内的持票客人。补偿不重新售票，仍在原票下办理，自发放之日起在后台配置的补偿有效期内有效
--    （业务参数，按项目可配，出厂默认一年，须同时配置上下限）。尚未核销的受影响人份或次份，只把原核销权延长到该有效期内使用，不增加原次数
--    ……故障发生时已经完成入场核销、且本场次履约受影响的人份或次份，另补一次完整票面权益，包括重新提供酒水、礼品……补偿记录必须关联原票、
--    对应人份或次份、原场次、游乐园整体故障事件和发放原因，同一故障下同一原人份或次份只补偿一次；批量发放前须由游乐公司负责人和工程或安全岗位共同确认。」

-- ① 补偿有效期参数：按综合体项目可配，出厂默认一年；上下限须同时配置（缺任一不得保存）。'__global__' 为出厂回落行。
CREATE TABLE IF NOT EXISTS amusement_compensation_policies (
    project_id VARCHAR(64) PRIMARY KEY,                -- 综合体项目；'__global__' 出厂回落行
    validity_days INTEGER NOT NULL,                    -- 补偿有效期（天），自发放之日起算
    min_days INTEGER NOT NULL,
    max_days INTEGER NOT NULL,
    updated_by VARCHAR(64),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT chk_amusement_compensation_policies_bounds CHECK (min_days >= 1 AND min_days <= validity_days AND validity_days <= max_days)
);
INSERT INTO amusement_compensation_policies (project_id, validity_days, min_days, max_days)
VALUES ('__global__', 365, 1, 3650)
ON CONFLICT (project_id) DO NOTHING;

-- ② 补偿批次：对某次全园故障事件的一次批量发放（发起＝快照受影响人份/次份；另一岗位确认＝实际发放）。
CREATE TABLE IF NOT EXISTS amusement_fault_compensation_batches (
    id VARCHAR(64) PRIMARY KEY,
    fault_id VARCHAR(64) NOT NULL REFERENCES amusement_park_wide_faults (id),
    project_id VARCHAR(64) NOT NULL,
    status VARCHAR(32) NOT NULL,                       -- pending_confirmation | issued
    reason TEXT NOT NULL,                              -- 发放原因
    bearer_company_id VARCHAR(64) NOT NULL,            -- 决定发放补偿的域主管公司的具体法律主体（承担本次完整补偿新增履约）
    validity_days INTEGER NOT NULL,                    -- 发起时锁定的补偿有效期（天）
    window_from TIMESTAMPTZ NOT NULL,                  -- 故障发生时点（全园故障状态生效时点）
    window_to TIMESTAMPTZ NOT NULL,                    -- 影响时间范围终点（解除时点，未解除取发起时点）
    valid_until TIMESTAMPTZ,                           -- 发放后：补偿有效期截止（发放日 + validity_days）
    initiated_by VARCHAR(64) NOT NULL,
    initiated_role VARCHAR(32) NOT NULL,
    initiated_at TIMESTAMPTZ NOT NULL,
    confirmed_by VARCHAR(64),
    confirmed_role VARCHAR(32),
    confirmed_at TIMESTAMPTZ,
    item_count INTEGER NOT NULL DEFAULT 0,
    extend_count INTEGER NOT NULL DEFAULT 0,
    reissue_count INTEGER NOT NULL DEFAULT 0,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT chk_amusement_fault_compensation_batches_status CHECK (status IN ('pending_confirmation', 'issued'))
);
-- 同一故障同一时刻只允许一个待确认批次。
CREATE UNIQUE INDEX IF NOT EXISTS uq_amusement_fault_compensation_batches_pending
    ON amusement_fault_compensation_batches (fault_id) WHERE status = 'pending_confirmation';
CREATE INDEX IF NOT EXISTS idx_amusement_fault_compensation_batches_fault
    ON amusement_fault_compensation_batches (fault_id, created_at DESC);

-- ③ 补偿记录：一行＝原票下一个受影响人份（或多次票的一个次份），关联原票、原人份/次份、原场次、故障事件与发放原因。
CREATE TABLE IF NOT EXISTS amusement_fault_compensation_items (
    id VARCHAR(64) PRIMARY KEY,
    batch_id VARCHAR(64) NOT NULL REFERENCES amusement_fault_compensation_batches (id),
    fault_id VARCHAR(64) NOT NULL REFERENCES amusement_park_wide_faults (id),
    project_id VARCHAR(64) NOT NULL,
    entitlement_id VARCHAR(64) NOT NULL REFERENCES issued_entitlements (id),   -- 原票下受影响的游乐类权益行（＝一个人份）
    order_id VARCHAR(64),
    ticket_type_id VARCHAR(64),
    parent_ticket_id VARCHAR(64),
    slot_index INTEGER,
    customer_id VARCHAR(64),
    kind VARCHAR(32) NOT NULL,                         -- extend（尚未核销：只延长原核销权）| full_reissue（已入场核销且本场次受影响：另补一次完整票面权益）
    share_count INTEGER NOT NULL DEFAULT 1,            -- 受影响次份数（多次票只补受影响的 1 次份）
    original_session_id VARCHAR(64),                   -- 原场次（已核销受影响人份）
    original_entry_verification_id VARCHAR(64),        -- 原入场核销记录（amusement_verifications.id）
    original_valid_to TIMESTAMPTZ,
    status VARCHAR(32) NOT NULL,                       -- pending | issued | used
    new_valid_to TIMESTAMPTZ,                          -- extend：延长后的核销权截止
    compensation_entitlement_ids JSONB NOT NULL DEFAULT '[]'::jsonb,  -- full_reissue：补发的权益行 id（原票下、完整票面）
    fulfilled_verification_id VARCHAR(64),             -- 补偿机会实际履约的核销记录（用一次即用尽）
    fulfilled_at TIMESTAMPTZ,
    fulfilled_settlement_price_cent BIGINT,            -- 补偿履约时生效的内部结算价（锁定到本记录）
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT chk_amusement_fault_compensation_items_kind CHECK (kind IN ('extend', 'full_reissue')),
    CONSTRAINT chk_amusement_fault_compensation_items_status CHECK (status IN ('pending', 'issued', 'used'))
);
-- 同一故障下同一原人份（权益行）只补偿一次。
CREATE UNIQUE INDEX IF NOT EXISTS uq_amusement_fault_compensation_items_once
    ON amusement_fault_compensation_items (fault_id, entitlement_id);
CREATE INDEX IF NOT EXISTS idx_amusement_fault_compensation_items_batch
    ON amusement_fault_compensation_items (batch_id, created_at);

-- ④ 补发的权益行标记：指向补偿记录；核销时据此「补偿使用不重复产生积分与成长值」、把履约锁定到补偿记录，且不计入原订单积分份额。
ALTER TABLE issued_entitlements ADD COLUMN IF NOT EXISTS compensation_item_id VARCHAR(64);
CREATE INDEX IF NOT EXISTS idx_issued_entitlements_compensation
    ON issued_entitlements (compensation_item_id) WHERE compensation_item_id IS NOT NULL;

-- ⑤ 权限点：补偿有效期参数配置（高敏）；发起/确认批次沿用 V446 两个岗位权限码；查询沿用 amusement.park_fault.view。
INSERT INTO permissions (permission_code, permission_name, domain, risk_level, requires_approval, status)
VALUES ('amusement.compensation_policy.manage', '配置全园故障补偿有效期参数(含上下限,高敏)', 'amusement', 'high', TRUE, 'active')
ON CONFLICT (permission_code) DO NOTHING;
INSERT INTO role_permissions (role_id, permission_code)
SELECT v.role_id, v.permission_code
FROM (VALUES ('role_platform_admin', 'amusement.compensation_policy.manage')) AS v(role_id, permission_code)
WHERE EXISTS (SELECT 1 FROM roles r WHERE r.id = v.role_id)
ON CONFLICT (role_id, permission_code) DO NOTHING;

DO $$
BEGIN
    IF EXISTS (SELECT 1 FROM pg_roles WHERE rolname = 'complex_app') THEN
        GRANT SELECT, INSERT, UPDATE, DELETE ON amusement_compensation_policies TO complex_app;
        GRANT SELECT, INSERT, UPDATE, DELETE ON amusement_fault_compensation_batches TO complex_app;
        GRANT SELECT, INSERT, UPDATE, DELETE ON amusement_fault_compensation_items TO complex_app;
    END IF;
END $$;
