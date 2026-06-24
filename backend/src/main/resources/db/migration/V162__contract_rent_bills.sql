-- V162 合同租金计费（② 履约闭环 S2，PRD phase6）：生效合同按 rent_terms 起租金应收，打通「合同↔费用」。
-- 镜像 V150 物业费 property_fee_bills 范式：逐（合同,期）计费留痕 + 联动生成 source_type='contract' 应收 ar_ap_items；
--   UNIQUE(contract_id, period) 幂等（同合同同期不重复挂账）。读权限/写权限复用 finance.ar_ap.manage（租金即 AR 应收）。
-- 金额口径：rent_terms->>'baseRentCent'（分，显式）为计费基数；缺该键的合同跳过（不猜单位，类比物业费跳过无面积商户）。

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
