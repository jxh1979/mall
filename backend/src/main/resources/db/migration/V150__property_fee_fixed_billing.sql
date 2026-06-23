-- V150 物业费固定计费（F-OPS-08）：废止 CAM 权重分摊 + 年度 true-up（多退少补），
-- 改为「每平米固定单价 × 商户面积」按月计费、不退不补。
-- 背景：原 CAM（V110 cam_allocations 按权重分摊 + V121 cam_trueup_runs/lines 年度多退少补）三层口径矛盾
--   （预收/实际/分摊）随 true-up 废止而消解。用户 2026-06 裁决：物业费按每平米固定价收取、不退不补。
-- 处置：
--   1) 旧 cam_allocations / cam_trueup_runs / cam_trueup_lines 表与历史数据【保留只读】，本迁移不删除；
--      应用层 FinanceService.allocateCam/runCamTrueUp 改抛 CAM_ALLOCATION_ABOLISHED / CAM_TRUEUP_ABOLISHED。
--   2) 新建 property_fee_rates（项目级、可按期费率；effective_period NULL=该项目默认费率）。
--   3) 新建 property_fee_bills（逐户计费留痕，UNIQUE(project_id,period,merchant_id) 幂等、不退不补）。
--   4) seed 权限 finance.property_fee.manage（费率维护 + 计费）。
-- 取数（只读跨域）：商户面积来自 merchants.area_square_meter（NUMERIC(18,2)，V005/V042）；merchants 无 project_id，
--   故计费由运营提供 merchantIds 列表（类比旧 CAM 提供 shares），费率按请求 project_id 解析。

-- ── 1) 物业费费率（项目级、可按期）─────────────────────────────────────────────
-- effective_period：精确计费期（如 '2026-06'）；NULL=该项目默认费率（精确期未命中时回退）。
-- rate_per_square_meter_cent：每平米单价（分 / ㎡ / 期）。
CREATE TABLE IF NOT EXISTS property_fee_rates (
    id VARCHAR(64) PRIMARY KEY,
    project_id VARCHAR(64) NOT NULL,
    effective_period VARCHAR(32),
    rate_per_square_meter_cent BIGINT NOT NULL,
    status VARCHAR(32) NOT NULL DEFAULT 'active',
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT chk_pfr_rate_nonneg CHECK (rate_per_square_meter_cent >= 0),
    CONSTRAINT chk_pfr_status CHECK (status IN ('active', 'inactive'))
);
-- 每 (项目, 计费期) 唯一一条费率；NULL period 视作同一「默认」键，避免重复默认费率。
CREATE UNIQUE INDEX IF NOT EXISTS uq_property_fee_rates_project_period
    ON property_fee_rates (project_id, COALESCE(effective_period, '__default__'));

-- ── 2) 物业费计费留痕（逐户，不退不补，幂等）─────────────────────────────────────
-- amount_cent = round(rate_per_square_meter_cent × area_square_meter)。
-- 一旦某 (项目,期,商户) 计费成立即固定（UNIQUE + 应用层不退不补），费率事后调整不影响已计费行。
CREATE TABLE IF NOT EXISTS property_fee_bills (
    id VARCHAR(64) PRIMARY KEY,
    project_id VARCHAR(64) NOT NULL,
    period VARCHAR(32) NOT NULL,
    merchant_id VARCHAR(64) NOT NULL,
    area_square_meter NUMERIC(18,2) NOT NULL,
    rate_per_square_meter_cent BIGINT NOT NULL,
    amount_cent BIGINT NOT NULL,
    ar_ap_item_id VARCHAR(64) REFERENCES ar_ap_items(id),
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT chk_pfb_area_pos CHECK (area_square_meter > 0),
    CONSTRAINT chk_pfb_rate_nonneg CHECK (rate_per_square_meter_cent >= 0),
    CONSTRAINT chk_pfb_amount_nonneg CHECK (amount_cent >= 0),
    CONSTRAINT uq_property_fee_bills_project_period_merchant UNIQUE (project_id, period, merchant_id)
);

CREATE INDEX IF NOT EXISTS idx_property_fee_bills_merchant_period
    ON property_fee_bills (merchant_id, period);

-- property_fee_bills 纳入 append-only（delete_only，复用 V140 complex_block_mutation()）：
-- 物业费计费是权威应收派生留痕，仅禁物理 DELETE 永久留痕（无合法 UPDATE、不退不补）。
DO $$
BEGIN
    IF to_regclass('property_fee_bills') IS NOT NULL THEN
        EXECUTE 'DROP TRIGGER IF EXISTS trg_append_only_d ON property_fee_bills';
        EXECUTE 'CREATE TRIGGER trg_append_only_d BEFORE DELETE ON property_fee_bills '
             || 'FOR EACH ROW EXECUTE FUNCTION complex_block_mutation()';
    END IF;
END $$;

-- 生产受限运行时 role 纵深：与 V140/V145 对 delete_only 表口径一致，REVOKE DELETE（role 不存在则跳过）。
DO $$
BEGIN
    IF to_regclass('property_fee_bills') IS NOT NULL
       AND EXISTS (SELECT 1 FROM pg_roles WHERE rolname = 'complex_app') THEN
        EXECUTE 'REVOKE DELETE ON property_fee_bills FROM complex_app';
    END IF;
END $$;

-- ── 3) seed 权限（参照 V110 finance 权限 INSERT 模式）─────────────────────────────
INSERT INTO permissions (permission_code, permission_name, domain, risk_level, requires_approval, status)
VALUES
    ('finance.property_fee.manage', 'Manage property management fee rates and billing', 'finance', 'high', TRUE, 'active')
ON CONFLICT (permission_code) DO UPDATE SET
    permission_name = EXCLUDED.permission_name,
    domain = EXCLUDED.domain,
    risk_level = EXCLUDED.risk_level,
    requires_approval = EXCLUDED.requires_approval,
    status = EXCLUDED.status;

INSERT INTO role_permissions (role_id, permission_code)
SELECT 'role_platform_admin', permission_code
  FROM permissions
 WHERE permission_code IN ('finance.property_fee.manage')
ON CONFLICT (role_id, permission_code) DO NOTHING;
