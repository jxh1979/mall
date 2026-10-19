-- 文件版本：V0.01
-- V509：FS-022-R1 积分发行费月账单接入统一开票清单。
-- 依据：ENUM L84「商户应收的兑换补偿与平台应收的积分发行费各自全额挂账、各自开票；一律按各自总额分别收付，不得轧差」；
--   BSM L1750「开票金额≤已收−已退−已开……开票由税务／电子发票第三方平台对接完成，本系统不自建开票引擎，只做状态对接与留痕」；
--   BSM L1803「平台内部各主体之间的往来需要开具发票」。
-- 清单条目来源二选一：月度结算单（bill_id）或积分月账单（points_bill_id）；积分行的购方＝账单合同销售方。
ALTER TABLE settlement_invoice_items ALTER COLUMN bill_id DROP NOT NULL;
ALTER TABLE settlement_invoice_items
    ADD COLUMN points_bill_id    VARCHAR(64) REFERENCES ticket_points_monthly_bills(id),
    ADD COLUMN buyer_entity_id   VARCHAR(64) REFERENCES business_entities(id),
    ADD COLUMN buyer_merchant_id VARCHAR(64) REFERENCES merchants(id),
    ADD CONSTRAINT chk_sii_one_source CHECK (num_nonnulls(bill_id, points_bill_id) = 1);
CREATE INDEX idx_sii_points_bill ON settlement_invoice_items (points_bill_id) WHERE points_bill_id IS NOT NULL;

-- 账单类型原名取 ENUM L256「积分发行费」；税率不在此种，沿用 settlement_tax_configs（未配即按 0 出清单）。
INSERT INTO invoice_subject_rules
    (id, income_type, revenue_stream, invoice_item_name, tax_category, evidence_required, invoiceable)
VALUES
    ('isr_points_issuance', 'points_issuance', NULL, '积分发行费', '积分发行费', '', TRUE)
ON CONFLICT (id) DO NOTHING;
