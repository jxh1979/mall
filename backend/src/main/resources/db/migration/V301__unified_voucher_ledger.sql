-- V301 积分/券统一台账（方案A，2026-07-18 拍板）：一张券台账收编"该收的"，通票权益并入展示不搬家。
-- 收编范围：①积分兑换券（redeemPoints 改为兑出券实例，不再直接扣库存/预约实物）＋②营销赠券
--   （本轮起新发券走统一台账；此前系统无营销赠券实现，无历史券需兼容读）。
--   通票权益/第三方合作券（V282 partner_voucher）不搬表，由"全部券"只读聚合端点（读层）并入展示。
-- 三类核销×结算（2026-07-08 拍板口径）：商户折扣券（结账抵扣，不结算）/店内实物券（核销扣库存，
--   按内部结算价挂应付，复用 PartnerVoucherSettlementPort 内部价范式）/外部伙伴券（V282 对账回流，不动）。
-- 状态机：issued → redeemed / expired / revoked（一次性券；通票权益是计次模型不硬套，读层做兼容映射）。

-- ① 券定义（模板）：名称/类型/核销方式/结算口径/面额或实物/有效期策略。
CREATE TABLE IF NOT EXISTS voucher_definitions (
    id varchar(64) PRIMARY KEY,
    name varchar(200) NOT NULL,
    -- merchant_discount=商户折扣券 in_store_goods=店内实物券 external_partner=外部伙伴券
    -- （external_partner 为模型完整性预留：本轮其实例仍在 V282 权益侧，统一表不发该类实例，服务端拒建。）
    voucher_kind varchar(32) NOT NULL,
    redeem_method varchar(32) NOT NULL,
    settlement_mode varchar(32) NOT NULL,
    merchant_id varchar(64),
    face_value_cent bigint,
    inventory_item_id varchar(64),
    stock_location_id varchar(64),
    stock_quantity int,
    internal_price_cent bigint,
    valid_days int,
    valid_until timestamptz,
    status varchar(16) NOT NULL DEFAULT 'active',
    created_at timestamptz NOT NULL DEFAULT now(),
    updated_at timestamptz NOT NULL DEFAULT now(),
    CONSTRAINT chk_vd_kind CHECK (voucher_kind IN ('merchant_discount', 'in_store_goods', 'external_partner')),
    CONSTRAINT chk_vd_method CHECK (redeem_method IN ('in_store_staff', 'onsite_code', 'external_reflow')),
    CONSTRAINT chk_vd_mode CHECK (settlement_mode IN ('none', 'internal_price', 'pass_settlement')),
    CONSTRAINT chk_vd_status CHECK (status IN ('active', 'disabled')),
    CONSTRAINT chk_vd_face_value CHECK (face_value_cent IS NULL OR face_value_cent > 0),
    CONSTRAINT chk_vd_internal_price CHECK (internal_price_cent IS NULL OR internal_price_cent > 0),
    CONSTRAINT chk_vd_stock_quantity CHECK (stock_quantity IS NULL OR stock_quantity > 0),
    CONSTRAINT chk_vd_valid_days CHECK (valid_days IS NULL OR valid_days > 0),
    -- 形状约束：折扣券必须有面额+归属商户；实物券必须有货品+库位+件数；内部结算必须有内部价+结算商户。
    CONSTRAINT chk_vd_discount_shape CHECK (voucher_kind <> 'merchant_discount'
        OR (face_value_cent IS NOT NULL AND merchant_id IS NOT NULL)),
    CONSTRAINT chk_vd_goods_shape CHECK (voucher_kind <> 'in_store_goods'
        OR (inventory_item_id IS NOT NULL AND stock_location_id IS NOT NULL AND stock_quantity IS NOT NULL)),
    CONSTRAINT chk_vd_internal_price_shape CHECK (settlement_mode <> 'internal_price'
        OR (internal_price_cent IS NOT NULL AND merchant_id IS NOT NULL))
);
CREATE INDEX IF NOT EXISTS idx_vd_merchant_status ON voucher_definitions (merchant_id, status);

-- ② 券实例：持有客人/核销码/状态机。收编来源＝积分兑换（points_redemption）＋营销赠发（marketing_grant）。
CREATE TABLE IF NOT EXISTS voucher_instances (
    id varchar(64) PRIMARY KEY,
    definition_id varchar(64) NOT NULL REFERENCES voucher_definitions(id),
    customer_id varchar(64) NOT NULL,
    code varchar(64) NOT NULL UNIQUE,
    source_type varchar(32) NOT NULL,
    -- points_redemption：兑换记录 id；marketing_grant：发放操作幂等派生 id。
    source_ref varchar(120),
    status varchar(16) NOT NULL DEFAULT 'issued',
    issued_at timestamptz NOT NULL,
    expires_at timestamptz,
    redeemed_at timestamptz,
    revoked_at timestamptz,
    revoke_reason varchar(500),
    created_at timestamptz NOT NULL DEFAULT now(),
    updated_at timestamptz NOT NULL DEFAULT now(),
    CONSTRAINT chk_vi_source CHECK (source_type IN ('points_redemption', 'marketing_grant')),
    CONSTRAINT chk_vi_status CHECK (status IN ('issued', 'redeemed', 'expired', 'revoked')),
    CONSTRAINT chk_vi_redeemed_shape CHECK (status <> 'redeemed' OR redeemed_at IS NOT NULL),
    CONSTRAINT chk_vi_revoked_shape CHECK (status <> 'revoked' OR (revoked_at IS NOT NULL AND revoke_reason IS NOT NULL))
);
CREATE INDEX IF NOT EXISTS idx_vi_customer_status ON voucher_instances (customer_id, status);
CREATE INDEX IF NOT EXISTS idx_vi_definition ON voucher_instances (definition_id);
CREATE INDEX IF NOT EXISTS idx_vi_source_ref ON voucher_instances (source_type, source_ref);

-- ③ 核销记录＋结算挂钩（一张表）：核销点/操作员/结算快照（内部结算价→应付 ar_ap_item）。
--   资金相关仅追加（V140 immutable 口径）：禁 UPDATE/DELETE；更正＝作废券/重发新券，不改历史核销行。
CREATE TABLE IF NOT EXISTS voucher_redemption_records (
    id varchar(120) PRIMARY KEY,
    voucher_instance_id varchar(64) NOT NULL REFERENCES voucher_instances(id),
    definition_id varchar(64) NOT NULL,
    voucher_kind varchar(32) NOT NULL,
    customer_id varchar(64) NOT NULL,
    merchant_id varchar(64),
    operator_user_id varchar(80),
    redeem_method varchar(32) NOT NULL,
    settlement_mode varchar(32) NOT NULL,
    settlement_amount_cent bigint,
    ar_ap_item_id varchar(64),
    stock_movement_ref varchar(120),
    -- 预留：将来第四种来源（抖音/美团团购券等）接 external_reflow 时的外部核销单号。
    external_redemption_ref varchar(128),
    note varchar(500),
    redeemed_at timestamptz NOT NULL,
    created_at timestamptz NOT NULL DEFAULT now(),
    CONSTRAINT chk_vrr_settlement_amount CHECK (settlement_amount_cent IS NULL OR settlement_amount_cent > 0)
);
-- 一次性券只允许核销一次：实例级唯一索引＝数据库层双花硬闸（服务层 CAS 之外的第二道）。
CREATE UNIQUE INDEX IF NOT EXISTS uq_vrr_instance ON voucher_redemption_records (voucher_instance_id);
CREATE INDEX IF NOT EXISTS idx_vrr_merchant_time ON voucher_redemption_records (merchant_id, redeemed_at DESC);

-- append-only 硬强制（V140 范式）：核销记录仅追加，禁 UPDATE 与 DELETE。
DO $$
BEGIN
    IF to_regclass('voucher_redemption_records') IS NOT NULL THEN
        EXECUTE 'DROP TRIGGER IF EXISTS trg_append_only_ud ON voucher_redemption_records';
        EXECUTE 'CREATE TRIGGER trg_append_only_ud BEFORE UPDATE OR DELETE ON voucher_redemption_records '
             || 'FOR EACH ROW EXECUTE FUNCTION complex_block_mutation()';
    END IF;
END $$;

-- ④ 兑换目录挂券模板：显式挂接优先；未挂接的目录项由服务端按 vdef_cat_<目录id> 派生定义（get-or-create）。
ALTER TABLE customer_redemption_catalog_items
    ADD COLUMN IF NOT EXISTS voucher_definition_id varchar(64) REFERENCES voucher_definitions(id);

-- ⑤ 积分兑换记录状态扩展：新路径只发券（voucher_issued，兑出统一台账券实例）；
--   旧状态（pending_pickup/picked_up/cancelled/refunded）保留，历史记录只读可见、旧核销领取仅对历史行有效。
ALTER TABLE customer_points_redemptions DROP CONSTRAINT IF EXISTS customer_points_redemptions_status_check;
ALTER TABLE customer_points_redemptions ADD CONSTRAINT customer_points_redemptions_status_check
    CHECK (status IN ('pending_pickup', 'picked_up', 'refunded', 'cancelled', 'voucher_issued'));

-- ⑥ 生产运行时授权（V297:50-55 同款；上轮 V296/V297 曾漏授直接生产 500，硬性检查项）：
--   每张新表显式 GRANT complex_app；核销记录与 V140 immutable 口径一致 REVOKE UPDATE/DELETE。
DO $$
BEGIN
    IF EXISTS (SELECT 1 FROM pg_roles WHERE rolname = 'complex_app') THEN
        EXECUTE 'GRANT SELECT, INSERT, UPDATE, DELETE ON voucher_definitions TO complex_app';
        EXECUTE 'GRANT SELECT, INSERT, UPDATE, DELETE ON voucher_instances TO complex_app';
        EXECUTE 'GRANT SELECT, INSERT ON voucher_redemption_records TO complex_app';
        EXECUTE 'REVOKE UPDATE, DELETE ON voucher_redemption_records FROM complex_app';
    END IF;
END $$;

-- ⑦ 权限种子（V279 范式）＋平台管理员直授（V206 范式）。其它角色按需另行授权。
INSERT INTO permissions (permission_code, permission_name, domain, risk_level, requires_approval, status)
VALUES
    ('customer.voucher.view', 'View unified voucher ledger (definitions, instances, redemptions, overview)', 'customer', 'medium', FALSE, 'active'),
    ('customer.voucher.manage', 'Manage unified voucher ledger (create definition, issue, revoke)', 'customer', 'high', FALSE, 'active'),
    ('customer.voucher.redeem', 'Redeem unified ledger vouchers (in-store discount deduction / goods pickup)', 'customer', 'medium', FALSE, 'active'),
    -- 内部结算价（成本）读脱敏权限（V279 bar.internal_settlement_price.view 同款铁律，2026-07-08 拍板）：
    -- 无此权限的读端 internal_price_cent / settlement_amount_cent 一律置 null；核销下放店员后成本不回吐。
    ('voucher.internal_price.view', 'View voucher internal settlement (cost) price', 'customer', 'high', FALSE, 'active')
ON CONFLICT (permission_code) DO NOTHING;

INSERT INTO role_permissions (role_id, permission_code)
SELECT 'role_platform_admin', code
  FROM (VALUES ('customer.voucher.view'), ('customer.voucher.manage'), ('customer.voucher.redeem'),
               ('voucher.internal_price.view')) AS seed(code)
ON CONFLICT (role_id, permission_code) DO NOTHING;
