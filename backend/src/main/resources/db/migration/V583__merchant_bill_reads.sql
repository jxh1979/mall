-- 文件版本：V0.01
-- 第十八批 K 段第 92 条 92-R1 商户专区账单已读。BSM:1597「商户专区首页保留未读账单提醒，该提醒不因异议期届满而消失。」
-- 按商户记（同商户任一账号在商户专区看过即算已读），首次已读时间不改；登录不算读；不设任何按时间清除。
-- ar_ap_item＝综合账单与门票分账所列本商户台账行（同一笔往来两处任一处看过即已读）；dividend_statement＝审批通过后的分红单。
-- ar_ap_items 是只增表，不往台账表加列。bill_id 宽度对 ar_ap_items.id（V273 放宽到 128）。
CREATE TABLE merchant_bill_reads (
    merchant_id VARCHAR(64)  NOT NULL,
    bill_kind   VARCHAR(32)  NOT NULL,
    bill_id     VARCHAR(128) NOT NULL,
    read_at     TIMESTAMPTZ  NOT NULL DEFAULT now(),
    PRIMARY KEY (merchant_id, bill_kind, bill_id),
    CONSTRAINT chk_merchant_bill_reads_kind CHECK (bill_kind IN ('ar_ap_item', 'dividend_statement'))
);

COMMENT ON TABLE merchant_bill_reads IS '商户专区账单已读（BSM:1597 首页未读账单提醒）：按商户记，首次已读时间不改，不随异议期届满清除';

DO $$
BEGIN
    IF EXISTS (SELECT 1 FROM pg_roles WHERE rolname = 'complex_app') THEN
        EXECUTE 'GRANT SELECT, INSERT, UPDATE, DELETE ON merchant_bill_reads TO complex_app';
    END IF;
END $$;
