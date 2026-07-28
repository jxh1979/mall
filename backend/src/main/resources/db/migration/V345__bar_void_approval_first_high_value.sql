-- V345 P0-C/FLOW-07 高价值已出品退酒「先审后行」(BAR-VOID-A1)。
--
-- 【为什么】v7 综合审计 P0-C：`BarOrderService.voidOrderItem` 一直是**先生效后审批**——
--   库存回补（atomicIncreaseStock + inbound 流水）、账单减项（waiver / 已结 refund 应付过账）、
--   明细标 voided **在发起 void 的那一刻就全部落地**，审批（decideOrderItemVoid）只是事后回写 status，
--   驳回才靠 reverseVoidSideEffects 反向冲销。等于「钱和库存先动，审批走在后面」。
--   业务口径 2026-07-27 已收口为 **A1**（`docs/business-decision-log-v0.1.md` BAR-VOID-A1 +
--   `docs/bar-business-operations-product-prd-v0.1.md` §10.2.1/§10.2.1A）：
--     ① 只对**已出品**（void_type=return）设审批闸，出品前 cancel 不受此限；
--     ② 高价值阈值默认 **5000 元**（500000 分）且**必须可配**（DEC-CFG-ALL-NUMERIC-001「时间与数字一律可配、禁止写死」）；
--     ③ 高价值 = 先审后行 + **双证据**（结构化原因码 + 附件，附件不可空）；
--     ④ 低于阈值即时生效（A1 明确允许），不得一刀切全拦。
--
-- 【本迁移做什么】只建配置底座 + 给 void 流水加「本次走哪条闸 / 副作用何时真正落地」的留痕列；
--   引擎（BarOrderService）随本批改为读配置并按 A1 分流。不改任何既有行的语义：
--   既有历史行 approval_mode 缺省 'immediate'（= 改造前的行为），逐行回看仍成立，可独立回滚。
--
-- 【范式】照结算配置中心 V327/V328/V331/V336/V340（项目作用域 + 商户维 + is_draft 草案位 +
--   部分唯一索引带 status），写入面/复核转正/软删复用既有 `settlement.config.manage`（高敏·差人复核）
--   与 SettlementConfigWriteService，不新增权限点。种子 is_draft=FALSE（生效态），读取侧强制 is_draft=FALSE。
--
-- 【缺配置行为】读取侧 fail-loud（同 JdbcCommissionPointsConfigRepository.requireConfigured）：
--   连 __DEFAULT__ 种子都查不到（种子被删/迁移未跑到位=部署坏）则响亮报错，**绝不静默套回 5000 元硬编码**——
--   「无配置即响亮停」正是根除硬编码回落的目的。正常态恒有 __DEFAULT__ 生效种子，故不触发。

-- ========== 1) 高价值退酒审批阈值（项目公司 × 商户）==========
CREATE TABLE IF NOT EXISTS bar_void_approval_configs (
    id                        VARCHAR(48) PRIMARY KEY,
    project_id                VARCHAR(64),          -- NULL=全局默认；非空=某项目公司特例
    merchant_id               VARCHAR(64),          -- NULL=该项目下所有商户（酒吧）通用；非空=某商户特例
    high_value_threshold_cent BIGINT      NOT NULL, -- 单笔已出品退品金额「超过」本值 = 高价值（分）；默认 500000=5000 元
    effective_from            DATE,
    effective_to              DATE,
    status                    VARCHAR(16) NOT NULL DEFAULT 'active',
    is_draft                  BOOLEAN     NOT NULL DEFAULT FALSE,
    archive_reason            TEXT,                 -- 软删铁律：下线留理由，不物理删
    updated_by                VARCHAR(64),
    updated_at                TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT chk_bvac_threshold CHECK (high_value_threshold_cent >= 0),
    CONSTRAINT chk_bvac_status    CHECK (status IN ('active', 'inactive'))
);
CREATE UNIQUE INDEX IF NOT EXISTS uq_bvac
    ON bar_void_approval_configs (
        COALESCE(project_id, '__DEFAULT__'),
        COALESCE(merchant_id, '__ANY__'),
        is_draft
    ) WHERE status = 'active';

-- 种子 = BAR-VOID-A1 默认口径：5000 元（500000 分），全局默认作用域、生效态。
INSERT INTO bar_void_approval_configs (id, project_id, merchant_id, high_value_threshold_cent, is_draft)
VALUES ('bvac_default', NULL, NULL, 500000, FALSE)
ON CONFLICT (id) DO NOTHING;

-- ========== 2) void 流水：记「走哪条闸 / 双证据原因码 / 阈值快照 / 副作用落地时点」==========
ALTER TABLE bar_order_item_voids
    -- immediate      = A1 低价值/未出品：库存账单在发起时即时生效（改造前的唯一行为，故为默认值，历史行语义不变）
    -- approval_first = A1 高价值已出品：先审后行，审批通过前不动库存/账单/明细
    ADD COLUMN IF NOT EXISTS approval_mode VARCHAR(24) NOT NULL DEFAULT 'immediate',
    -- 双证据之一：结构化原因码（另一为 evidence_attachment_ids 附件，不可空）。approval_first 必填。
    ADD COLUMN IF NOT EXISTS reason_code VARCHAR(64),
    -- 判定当刻的阈值快照（分）。阈值可配、会随运营调整；留快照才能事后举证「当时按什么口径判的」。
    ADD COLUMN IF NOT EXISTS high_value_threshold_cent BIGINT,
    -- 库存/账单副作用真正落地的时点：immediate=发起时刻；approval_first=审批通过时刻；未落地=NULL。
    ADD COLUMN IF NOT EXISTS effects_applied_at TIMESTAMPTZ;

DO $$
BEGIN
    IF EXISTS (SELECT 1 FROM information_schema.table_constraints
                WHERE constraint_name = 'chk_bar_order_item_voids_approval_mode') THEN
        ALTER TABLE bar_order_item_voids DROP CONSTRAINT chk_bar_order_item_voids_approval_mode;
    END IF;
    ALTER TABLE bar_order_item_voids
        ADD CONSTRAINT chk_bar_order_item_voids_approval_mode CHECK (
            approval_mode IN ('immediate', 'approval_first')
        );
END $$;

-- 双证据（DB 级兜底，服务层已先拦）：先审后行的高价值退酒必须同时具备结构化原因码与非空附件。
DO $$
BEGIN
    IF EXISTS (SELECT 1 FROM information_schema.table_constraints
                WHERE constraint_name = 'chk_bar_order_item_voids_dual_evidence') THEN
        ALTER TABLE bar_order_item_voids DROP CONSTRAINT chk_bar_order_item_voids_dual_evidence;
    END IF;
    ALTER TABLE bar_order_item_voids
        ADD CONSTRAINT chk_bar_order_item_voids_dual_evidence CHECK (
            approval_mode <> 'approval_first'
            OR (reason_code IS NOT NULL
                AND evidence_attachment_ids IS NOT NULL
                AND jsonb_array_length(evidence_attachment_ids) >= 1)
        );
END $$;

-- 🔴 先审后行硬保证（资金级）：approval_first 且尚在 pending 时，**不得**存在任何副作用凭证——
--    没有库存流水 id、没有账单调整 id、副作用落地时点为空。服务层若被改坏，这条约束在 DB 层再挡一次。
DO $$
BEGIN
    IF EXISTS (SELECT 1 FROM information_schema.table_constraints
                WHERE constraint_name = 'chk_bar_order_item_voids_pending_no_effect') THEN
        ALTER TABLE bar_order_item_voids DROP CONSTRAINT chk_bar_order_item_voids_pending_no_effect;
    END IF;
    ALTER TABLE bar_order_item_voids
        ADD CONSTRAINT chk_bar_order_item_voids_pending_no_effect CHECK (
            approval_mode <> 'approval_first'
            OR status <> 'pending'
            OR (inventory_movement_id IS NULL AND bill_adjustment_id IS NULL AND effects_applied_at IS NULL)
        );
END $$;

-- 驳回的先审后行申请永远不落副作用（无冲销可言）：rejected + approval_first ⇒ effects_applied_at 必空。
DO $$
BEGIN
    IF EXISTS (SELECT 1 FROM information_schema.table_constraints
                WHERE constraint_name = 'chk_bar_order_item_voids_rejected_no_effect') THEN
        ALTER TABLE bar_order_item_voids DROP CONSTRAINT chk_bar_order_item_voids_rejected_no_effect;
    END IF;
    ALTER TABLE bar_order_item_voids
        ADD CONSTRAINT chk_bar_order_item_voids_rejected_no_effect CHECK (
            approval_mode <> 'approval_first'
            OR status <> 'rejected'
            OR effects_applied_at IS NULL
        );
END $$;

-- 待审队列（主管审批面）：按状态 + 时间倒序取待审申请。
CREATE INDEX IF NOT EXISTS idx_bar_order_item_voids_status
    ON bar_order_item_voids (status, created_at DESC);

-- ========== 3) 新表一律授权受限运行时（V296/V298/V338 血的教训：漏 GRANT → 运行时静默 permission denied）==========
-- 幂等：GRANT 可重复；运行时角色缺失（部分测试环境）时跳过（与 V140/V338/V340 同守卫）。
DO $$
BEGIN
    IF EXISTS (SELECT 1 FROM pg_roles WHERE rolname = 'complex_app') THEN
        EXECUTE 'GRANT SELECT, INSERT, UPDATE, DELETE ON bar_void_approval_configs TO complex_app';
    ELSE
        RAISE NOTICE 'skip GRANT to complex_app (restricted runtime role absent in this environment)';
    END IF;
END $$;
