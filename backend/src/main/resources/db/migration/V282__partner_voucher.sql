-- 批3:第三方券(通票包含·餐厅自己系统核销+对账文件回流)。免费营销券核销不结算;内部价采购券核销按内部结算价挂应付给第三方(商户或外部伙伴)。
CREATE TABLE partner_voucher_configs (
    entitlement_template_id varchar(64) PRIMARY KEY REFERENCES entitlement_templates(id) ON DELETE CASCADE,
    settlement_mode varchar(32) NOT NULL,
    partner_kind varchar(32) NOT NULL,
    partner_id varchar(64) NOT NULL,
    internal_price_cent bigint,
    created_at timestamptz NOT NULL DEFAULT now(),
    CONSTRAINT chk_pvc_mode CHECK (settlement_mode IN ('free_marketing','internal_purchased')),
    CONSTRAINT chk_pvc_kind CHECK (partner_kind IN ('merchant','external_partner')),
    CONSTRAINT chk_pvc_price CHECK (internal_price_cent IS NULL OR internal_price_cent >= 0)
);
CREATE TABLE partner_voucher_redemptions (
    id varchar(64) PRIMARY KEY,
    issued_entitlement_id varchar(64) NOT NULL,
    entitlement_code varchar(128) NOT NULL,
    partner_kind varchar(32) NOT NULL,
    partner_id varchar(64) NOT NULL,
    settlement_mode varchar(32) NOT NULL,
    redeemed_at timestamptz NOT NULL,
    external_redemption_ref varchar(128),
    settlement_amount_cent bigint,
    ar_ap_item_id varchar(64),
    source varchar(32) NOT NULL,
    created_at timestamptz NOT NULL DEFAULT now(),
    CONSTRAINT uq_pvr_ext UNIQUE (partner_id, external_redemption_ref)
);
CREATE INDEX idx_pvr_entitlement ON partner_voucher_redemptions(issued_entitlement_id);
GRANT SELECT, INSERT, UPDATE, DELETE ON partner_voucher_configs TO complex_app;
GRANT SELECT, INSERT, UPDATE, DELETE ON partner_voucher_redemptions TO complex_app;
