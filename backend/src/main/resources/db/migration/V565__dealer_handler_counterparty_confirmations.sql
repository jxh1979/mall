-- 文件版本：V0.01
-- 第十八批第 31 条（BSM:1247「合同销售主体指定经办人，通过已核验的企业联系人取得签收、退货等确认并留证，经办人不得代替对方确认」；
-- CG 第 67 条操作员答「另开 CG 建经办人确认留证对象」）：经销合同记指定经办人；经销商停用后，经办人线下向已核验联系人取得对方确认、
-- 系统留证（确认人取经销商档案已核验联系人快照，不收前端填写；须上传对方确认凭证）；签收、退货、运费、交付差异、超收用途各用一次。
ALTER TABLE dealer_contracts ADD COLUMN designated_handler_user_id VARCHAR(64);
ALTER TABLE dealer_contracts ADD COLUMN designated_handler_by VARCHAR(64);
ALTER TABLE dealer_contracts ADD COLUMN designated_handler_at TIMESTAMPTZ;

CREATE TABLE dealer_counterparty_confirmations (
    id                      VARCHAR(64) PRIMARY KEY,
    project_id              VARCHAR(64) NOT NULL,
    dealer_id               VARCHAR(64) NOT NULL REFERENCES dealers(id),
    order_id                VARCHAR(64) NOT NULL REFERENCES dealer_orders(id),
    matter_type             VARCHAR(32) NOT NULL CHECK (matter_type IN ('delivery_receipt', 'return_request', 'freight_confirmation',
                                'delivery_resolution', 'overpayment_use', 'result_delivery')),
    related_object_id       VARCHAR(128),
    contact_name            VARCHAR(100) NOT NULL,
    content                 TEXT NOT NULL,
    confirmed_at            TIMESTAMPTZ NOT NULL,
    evidence_attachment_ids JSONB NOT NULL CHECK (jsonb_array_length(evidence_attachment_ids) > 0),
    handler_user_id         VARCHAR(64) NOT NULL,
    used_by_id              VARCHAR(64),
    used_at                 TIMESTAMPTZ,
    created_at              TIMESTAMPTZ NOT NULL
);
CREATE INDEX idx_dealer_counterparty_confirmations_order ON dealer_counterparty_confirmations(order_id);
