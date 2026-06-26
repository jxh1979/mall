-- V162 合同租赁全生命周期（据需求重建·feat/real-estate-rebuild-from-requirements）
-- 依据：contract-lifecycle-prd-v1.0（V1.1）+ 业务HTML §九（合同状态8态/合作模式五类/四类主体）+ M1需求采集§2 + 用户裁决（可配+分组并行审批/可配到期着色）。
-- 替换旧 V161/162/163 合同骨架：合同状态收口为 HTML 8 业务态；新增 可配分组并行审批链 + 抽成4模式 + 生效固化快照 + 变更6类 + 退租逐例结算/争议挂起 + 铺位到期着色分类。
-- contracts 基表来自 V010（lifecycle_status 无 CHECK，dev 存量 0 行 → 改 CHECK 无违例）；contract_spaces 来自 V080；rent_terms(JSONB,V010) 承载 基础租金/递增/免租/物业费。
-- 数据隔离=综合体级，新表均带 project_id（配置表 project_id 可空=平台默认）。

-- ============================================================
-- 1) contracts：合同状态收口 8 业务态 + 补业务列（抽成/能源费分摊/担保/固化快照/退租留痕/续约链）
-- ============================================================
ALTER TABLE contracts ADD COLUMN IF NOT EXISTS commission_mode VARCHAR(48);            -- 抽成模式（4类，可空=无抽成）
ALTER TABLE contracts ADD COLUMN IF NOT EXISTS energy_fee_basis VARCHAR(32) NOT NULL DEFAULT 'none'; -- 公区能耗分摊（4模式，连能耗域）
ALTER TABLE contracts ADD COLUMN IF NOT EXISTS deposit_cent BIGINT;                    -- 保证金（分）
ALTER TABLE contracts ADD COLUMN IF NOT EXISTS penalty_terms JSONB NOT NULL DEFAULT '{}'::jsonb; -- 违约金条款
ALTER TABLE contracts ADD COLUMN IF NOT EXISTS settlement_rules JSONB NOT NULL DEFAULT '[]'::jsonb; -- 退租结算逐例顺序（每份可不同）
ALTER TABLE contracts ADD COLUMN IF NOT EXISTS snapshot JSONB;                         -- 生效固化收益快照（4项；NULL=未生效）
ALTER TABLE contracts ADD COLUMN IF NOT EXISTS project_id VARCHAR(64);                 -- 综合体维度
ALTER TABLE contracts ADD COLUMN IF NOT EXISTS terminated_at TIMESTAMPTZ;              -- 退租/终止留痕
ALTER TABLE contracts ADD COLUMN IF NOT EXISTS termination_reason VARCHAR(500);
ALTER TABLE contracts ADD COLUMN IF NOT EXISTS renewed_from_contract_id VARCHAR(64);   -- 续约链

-- 抽成模式 4 类（M1 §2，可空）
ALTER TABLE contracts DROP CONSTRAINT IF EXISTS chk_contracts_commission_mode;
ALTER TABLE contracts ADD CONSTRAINT chk_contracts_commission_mode
    CHECK (commission_mode IS NULL OR commission_mode IN
        ('guarantee_or_commission_higher', 'fixed_ratio', 'tiered_ratio', 'guarantee_plus_commission'));

-- 公区能耗分摊 4 模式（M1 §6，连合同生效口径）
ALTER TABLE contracts DROP CONSTRAINT IF EXISTS chk_contracts_energy_fee_basis;
ALTER TABLE contracts ADD CONSTRAINT chk_contracts_energy_fee_basis
    CHECK (energy_fee_basis IN ('none', 'by_contract', 'by_area', 'by_usage'));

-- 合同状态收口 HTML §九 8 业务态（替换旧技术态 draft/approved/rejected/returned/effective/expired/terminated）
ALTER TABLE contracts DROP CONSTRAINT IF EXISTS chk_contracts_lifecycle_status;
ALTER TABLE contracts ALTER COLUMN lifecycle_status SET DEFAULT 'drafting';
ALTER TABLE contracts ADD CONSTRAINT chk_contracts_lifecycle_status
    CHECK (lifecycle_status IN
        ('drafting',            -- 草拟
         'pending_confirmation',-- 待确认（审批链推进中）
         'pending_effective',   -- 待生效（全阶段通过、未到生效日）
         'effective',           -- 已生效（已固化快照）
         'renewing',            -- 续签中
         'expired',             -- 已到期
         'terminated',          -- 已终止（退租/提前终止）
         'archived'));          -- 归档

-- contract_spaces（V080）补综合体维度 + 索引（旧 V161 路径，重建保留）
ALTER TABLE contract_spaces ADD COLUMN IF NOT EXISTS project_id VARCHAR(64);
CREATE INDEX IF NOT EXISTS idx_contract_spaces_contract ON contract_spaces (contract_id);

-- ============================================================
-- 2) 可配置审批链（分组并行 + 可串接）：flow=链，steps 同 (flow,stage_seq) = 一个并行阶段（会签）
-- ============================================================
CREATE TABLE IF NOT EXISTS contract_approval_flows (
    id VARCHAR(64) PRIMARY KEY,
    project_id VARCHAR(64),                 -- NULL = 平台默认链；非空 = 某综合体覆盖
    name VARCHAR(160) NOT NULL,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now()
);
-- 每综合体至多一条启用链（NULL project 为平台默认）
CREATE UNIQUE INDEX IF NOT EXISTS uq_contract_approval_flows_project_active
    ON contract_approval_flows (COALESCE(project_id, '__default__')) WHERE active;

CREATE TABLE IF NOT EXISTS contract_approval_flow_steps (
    id VARCHAR(64) PRIMARY KEY,
    flow_id VARCHAR(64) NOT NULL REFERENCES contract_approval_flows(id),
    stage_seq INT NOT NULL,                 -- 阶段序（升序串行）；同序内多行 = 并行会签
    stage_name VARCHAR(80) NOT NULL,
    approver_permission_code VARCHAR(120) NOT NULL, -- 该阶段审批岗位的权限码（谁能审）
    active BOOLEAN NOT NULL DEFAULT TRUE,
    CONSTRAINT uq_caf_steps UNIQUE (flow_id, stage_seq, approver_permission_code)
);
CREATE INDEX IF NOT EXISTS idx_caf_steps_flow ON contract_approval_flow_steps (flow_id, stage_seq);

-- 合同审批实例留痕（按所属链逐阶段并行走）：当前 stage_seq 全 approved 才进下一阶段，任一 rejected 整单退回。
-- change_id 空 = 新签合同审批；非空 = 该合同变更走同一套可配并行链（复用审批引擎）。
CREATE TABLE IF NOT EXISTS contract_approvals (
    id VARCHAR(64) PRIMARY KEY,
    contract_id VARCHAR(64) NOT NULL REFERENCES contracts(id),
    change_id VARCHAR(64),
    stage_seq INT NOT NULL,
    approver_permission_code VARCHAR(120) NOT NULL,
    approver_user_id VARCHAR(64),
    decision VARCHAR(16) NOT NULL DEFAULT 'pending',
    comment VARCHAR(1000),
    decided_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT chk_contract_approvals_decision CHECK (decision IN ('pending', 'approved', 'rejected'))
);
CREATE INDEX IF NOT EXISTS idx_contract_approvals_contract ON contract_approvals (contract_id, change_id, stage_seq);

-- ============================================================
-- 3) 合同变更（6类，删转租；全走审批变更流）
-- ============================================================
CREATE TABLE IF NOT EXISTS contract_changes (
    id VARCHAR(64) PRIMARY KEY,
    contract_id VARCHAR(64) NOT NULL REFERENCES contracts(id),
    change_type VARCHAR(32) NOT NULL,
    payload JSONB NOT NULL DEFAULT '{}'::jsonb,
    approval_status VARCHAR(16) NOT NULL DEFAULT 'pending',
    new_snapshot JSONB,                     -- 变更生效后的新版本快照（不改历史固化）
    project_id VARCHAR(64),
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT chk_contract_changes_type CHECK (change_type IN
        ('renewal', 'move_out', 'area_change', 'rent_change', 'business_scope_change', 'early_termination')),
    CONSTRAINT chk_contract_changes_approval CHECK (approval_status IN ('pending', 'approved', 'rejected'))
);
CREATE INDEX IF NOT EXISTS idx_contract_changes_contract ON contract_changes (contract_id, created_at DESC);

-- ============================================================
-- 4) 退租/到期结算（逐例顺序 + 争议挂起）
-- ============================================================
CREATE TABLE IF NOT EXISTS contract_settlements (
    id VARCHAR(64) PRIMARY KEY,
    contract_id VARCHAR(64) NOT NULL REFERENCES contracts(id),
    trigger_type VARCHAR(24) NOT NULL,
    status VARCHAR(16) NOT NULL DEFAULT 'in_progress',
    settlement_items JSONB NOT NULL DEFAULT '[]'::jsonb, -- 逐例顺序执行结果（保证金抵扣→违约金→欠费→水电…）
    dispute_reason VARCHAR(500),
    resolved_by VARCHAR(64),
    resolved_at TIMESTAMPTZ,
    project_id VARCHAR(64),
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT chk_contract_settlements_trigger CHECK (trigger_type IN ('move_out', 'expiry', 'early_termination')),
    CONSTRAINT chk_contract_settlements_status CHECK (status IN ('in_progress', 'disputed', 'settled'))
);
CREATE INDEX IF NOT EXISTS idx_contract_settlements_contract ON contract_settlements (contract_id);

-- ============================================================
-- 5) 合同租金计费（复用旧 V162 范式：逐(合同,期)计费 + 联动 ar_ap_items 应收，UNIQUE 幂等）
-- ============================================================
CREATE TABLE IF NOT EXISTS contract_rent_bills (
    id VARCHAR(64) PRIMARY KEY,
    contract_id VARCHAR(64) NOT NULL REFERENCES contracts(id),
    merchant_id VARCHAR(64) NOT NULL,
    project_id VARCHAR(64),
    period VARCHAR(32) NOT NULL,
    amount_cent BIGINT NOT NULL,
    ar_ap_item_id VARCHAR(64) REFERENCES ar_ap_items(id),
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT chk_crb_amount_pos CHECK (amount_cent > 0),
    CONSTRAINT uq_contract_rent_bills_contract_period UNIQUE (contract_id, period)
);
CREATE INDEX IF NOT EXISTS idx_contract_rent_bills_merchant ON contract_rent_bills (merchant_id, period);

-- ============================================================
-- 6) 铺位到期着色分类（可配：每类 = 分类名 + 颜色 + 提前天数；铺位按距到期命中最紧一档）
-- ============================================================
CREATE TABLE IF NOT EXISTS space_expiry_color_rules (
    id VARCHAR(64) PRIMARY KEY,
    project_id VARCHAR(64),                 -- NULL = 平台默认
    category_name VARCHAR(80) NOT NULL,
    color VARCHAR(32) NOT NULL,             -- 十六进制/色名
    threshold_days INT NOT NULL,            -- 到期前 N 天亮起
    seq INT NOT NULL DEFAULT 0,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    CONSTRAINT chk_secr_threshold CHECK (threshold_days > 0)
);
CREATE INDEX IF NOT EXISTS idx_secr_project ON space_expiry_color_rules (project_id, active);

-- ============================================================
-- 7) 权限点（最小颗粒，仿 V010/V161 contract.* 风格）
-- ============================================================
INSERT INTO permissions (permission_code, permission_name, domain, risk_level, requires_approval, status)
VALUES
    ('contract.draft.manage',             'Draft/edit contract terms',                       'platform', 'medium', FALSE, 'active'),
    ('contract.approve.business',         'Contract approval - business review',             'platform', 'medium', FALSE, 'active'),
    ('contract.approve.finance',          'Contract approval - finance review',              'platform', 'medium', FALSE, 'active'),
    ('contract.approve.legal',            'Contract approval - legal review',                'platform', 'medium', FALSE, 'active'),
    ('contract.approve.gm',               'Contract approval - GM final review',             'platform', 'high',   FALSE, 'active'),
    ('contract.approval.flow.manage',     'Configure contract approval flow (stages/parallel)', 'platform', 'high', TRUE, 'active'),
    ('contract.change.manage',            'Raise contract change (6 types, via approval)',   'platform', 'medium', FALSE, 'active'),
    ('contract.settlement.manage',        'Move-out/expiry settlement (per-contract rules)', 'platform', 'high',   TRUE,  'active'),
    ('contract.settlement.dispute.resolve','Resolve settlement dispute (supervisor)',        'platform', 'high',   TRUE,  'active'),
    ('contract.space.color.manage',       'Configure space expiry color categories',         'platform', 'medium', FALSE, 'active')
ON CONFLICT (permission_code) DO UPDATE SET
    permission_name = EXCLUDED.permission_name, domain = EXCLUDED.domain,
    risk_level = EXCLUDED.risk_level, requires_approval = EXCLUDED.requires_approval, status = EXCLUDED.status;

INSERT INTO role_permissions (role_id, permission_code)
SELECT 'role_platform_admin', permission_code FROM permissions
 WHERE permission_code IN ('contract.draft.manage','contract.approve.business','contract.approve.finance',
        'contract.approve.legal','contract.approve.gm','contract.approval.flow.manage','contract.change.manage',
        'contract.settlement.manage','contract.settlement.dispute.resolve','contract.space.color.manage')
ON CONFLICT (role_id, permission_code) DO NOTHING;

-- ============================================================
-- 8) 种子：平台默认审批链（阶段①业务/财务/法务 并行会签 → 阶段②总经理终审）+ 默认到期着色（30天橙）
-- ============================================================
INSERT INTO contract_approval_flows (id, project_id, name, active)
VALUES ('caf_default', NULL, '平台默认合同审批链', TRUE)
ON CONFLICT (id) DO NOTHING;

INSERT INTO contract_approval_flow_steps (id, flow_id, stage_seq, stage_name, approver_permission_code, active)
VALUES
    ('cafs_default_biz',     'caf_default', 1, '业务审核',   'contract.approve.business', TRUE),
    ('cafs_default_fin',     'caf_default', 1, '财务审核',   'contract.approve.finance',  TRUE),
    ('cafs_default_legal',   'caf_default', 1, '法务审核',   'contract.approve.legal',    TRUE),
    ('cafs_default_gm',      'caf_default', 2, '总经理终审', 'contract.approve.gm',       TRUE)
ON CONFLICT (id) DO NOTHING;

INSERT INTO space_expiry_color_rules (id, project_id, category_name, color, threshold_days, seq, active)
VALUES ('secr_default_30', NULL, '即将到期', '#F59E0B', 30, 1, TRUE)
ON CONFLICT (id) DO NOTHING;
