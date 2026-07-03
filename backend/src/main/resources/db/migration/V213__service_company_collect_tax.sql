-- H批(2026-07-02 最终拍板,推翻直付):服务费付到公司统一账户(一期=公司收款码,聚合支付回调
-- 二期 gating);服务员现场确认收款(mark-paid)与完成;舞者工作无手机,端上只读;
-- 完成时分账:管理费(按目录比例/固定)归公司,余额写提成台账归舞者→月结进薪资;
-- 月结对当月服务劳务合计按劳务报酬预扣预缴法定税表代扣个税。
ALTER TABLE service_catalog_items ADD COLUMN IF NOT EXISTS mgmt_fee_mode VARCHAR(16) NOT NULL DEFAULT 'none';
ALTER TABLE service_catalog_items ADD COLUMN IF NOT EXISTS mgmt_fee_value BIGINT NOT NULL DEFAULT 0;
ALTER TABLE service_catalog_items ADD CONSTRAINT chk_svc_mgmt_mode CHECK (mgmt_fee_mode IN ('none', 'percent', 'fixed'));
ALTER TABLE service_catalog_items ADD CONSTRAINT chk_svc_mgmt_value CHECK (mgmt_fee_value >= 0);

ALTER TABLE service_orders ADD COLUMN IF NOT EXISTS paid_at TIMESTAMPTZ;
ALTER TABLE service_orders ADD COLUMN IF NOT EXISTS paid_by VARCHAR(64);
ALTER TABLE service_orders ADD COLUMN IF NOT EXISTS pay_method VARCHAR(24);
ALTER TABLE service_orders ADD COLUMN IF NOT EXISTS refund_note VARCHAR(255);
ALTER TABLE service_orders ADD COLUMN IF NOT EXISTS mgmt_fee_cent BIGINT NOT NULL DEFAULT 0;
ALTER TABLE service_orders ADD COLUMN IF NOT EXISTS dancer_share_cent BIGINT NOT NULL DEFAULT 0;
ALTER TABLE service_orders DROP CONSTRAINT IF EXISTS chk_svc_order_status;
ALTER TABLE service_orders ADD CONSTRAINT chk_svc_order_status
    CHECK (status IN ('created', 'paid', 'pending_accept', 'accepted', 'completed', 'cancelled'));

CREATE TABLE IF NOT EXISTS service_pay_config (
    id VARCHAR(16) PRIMARY KEY,
    qr_content VARCHAR(500) NOT NULL DEFAULT '',
    updated_by VARCHAR(64),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now()
);
INSERT INTO service_pay_config (id, qr_content) VALUES ('default', '')
ON CONFLICT (id) DO NOTHING;

ALTER TABLE staff_payroll_statements ADD COLUMN IF NOT EXISTS tax_cent BIGINT NOT NULL DEFAULT 0;
