-- 文件版本：V0.01
-- V680：第十八批 AZ 段第 290 条 290-R1、290-R2（平台内部主体之间的往来进开票清单）。
-- 依据：BSM:1803「平台内部各主体之间的往来需要开具发票，开票的具体规则一律按国家财税相关规定执行」；BSM:1750「开票金额≤已收−已退−已开」；
--   BSM:1760「税率、税目、发票对象和红冲流程由系统按有效规则配置并保留凭证关联」；决策台账 DEC-R4-2026-08-04-51。
-- ① invoice_subject_rules 补零售规则：组合通票零售业态份额（货物）；收入流为空＝开票主体取该业态份额归属主体。
--    发票项目名「零售商品」、税目口径「零售」按常理定（税目沿用 EntityTaxService 收入类型原名），税率由财务在税率配置按主体与 retail 配。
-- ② settlement_invoice_items 加 ar_ap_item_id：传媒演出结算应付（门票加价分配、固定演出费、酒水提成）行的来源台账；来源三选一
--    （结算单、积分月账单、台账行）；同一台账行在占额三态（待开票、开票中、已开票）内至多一条，挡并发重复生成。
-- 研发阶段无存量（UD-07）。
INSERT INTO invoice_subject_rules
    (id, income_type, revenue_stream, invoice_item_name, tax_category, evidence_required, invoiceable)
VALUES
    ('isr_retail', 'retail', NULL, '零售商品', '零售', '对账单', TRUE)
ON CONFLICT (id) DO NOTHING;

ALTER TABLE settlement_invoice_items
    ADD COLUMN ar_ap_item_id VARCHAR(64) REFERENCES ar_ap_items(id),
    DROP CONSTRAINT chk_sii_one_source,
    ADD CONSTRAINT chk_sii_one_source CHECK (num_nonnulls(bill_id, points_bill_id, ar_ap_item_id) = 1);

CREATE UNIQUE INDEX IF NOT EXISTS uq_sii_arap_open
    ON settlement_invoice_items (ar_ap_item_id)
 WHERE ar_ap_item_id IS NOT NULL AND status IN ('pending', 'issuing', 'issued');
