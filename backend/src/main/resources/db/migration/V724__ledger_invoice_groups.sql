-- 文件版本：V0.01
-- V724：第十八批 BU 段第 374 条 374-R1～R5（平台内部主体之间其余往来进开票清单：单店门票综合服务费、门店要货货款、营销与派驻提成拨付、
--   酒吧报酬拨付、停车抵免成本、通用卡消费结算）。
-- 依据：BSM:1803「平台内部各主体之间的往来需要开具发票，开票的具体规则一律按国家财税相关规定执行，本文件不复述细则。」；
--   BSM:1750「开票金额≤已收−已退−已开」「退款后先红冲再重开」；BSM:1760「税率、税目、发票对象和红冲流程由系统按有效规则配置并保留凭证关联」；
--   决策台账 DEC-R4-2026-08-04-51。
-- ① settlement_invoice_items 加台账来源月度组：ledger_source（台账来源类型）、ledger_group_key（来源×主体×月的组键）；来源四选一
--    （结算单、积分月账单、演出结算台账行、台账来源月度组）。同组晚到的流水另出差额行，故不建占额唯一索引（同积分月账单只补开新增部分）。
-- ② invoice_subject_rules 补三条规则：供货货款（门店要货货款，收入流 supply_goods）、停车费（停车抵免成本）、人员报酬拨付（派驻提成与酒吧报酬拨付）。
--    发票项目名与税目口径按常理定：「供货货款」「停车费」沿用 EntityTaxService 收入类型原名，「人员报酬拨付」取 BSM:1143 用词；凭证要件「对账单」同 V680。
-- 研发阶段无存量（UD-07）。
INSERT INTO invoice_subject_rules
    (id, income_type, revenue_stream, invoice_item_name, tax_category, evidence_required, invoiceable)
VALUES
    ('isr_goods', 'goods', 'supply_goods', '供货货款', '供货货款', '对账单', TRUE),
    ('isr_parking', 'parking', NULL, '停车费', '停车费', '对账单', TRUE),
    ('isr_staff_remuneration', 'staff_remuneration', NULL, '人员报酬拨付', '人员报酬拨付', '对账单', TRUE)
ON CONFLICT (id) DO NOTHING;

ALTER TABLE settlement_invoice_items
    ADD COLUMN ledger_source VARCHAR(40),
    ADD COLUMN ledger_group_key VARCHAR(64),
    DROP CONSTRAINT chk_sii_one_source,
    ADD CONSTRAINT chk_sii_one_source CHECK (num_nonnulls(bill_id, points_bill_id, ar_ap_item_id, ledger_group_key) = 1);

CREATE INDEX IF NOT EXISTS idx_sii_ledger_group
    ON settlement_invoice_items (ledger_group_key) WHERE ledger_group_key IS NOT NULL;
