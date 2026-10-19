-- 文件版本：V0.01
-- V505：FS-012 补偿新增实际履约逐笔记录与承担方补偿应付。
-- 依据：BSM L800 / ENUM L148「本次补偿新增的实际履约由决定发放补偿的具体域主管公司承担……本次完整补偿新增实际履约所用的内部结算价
--   及其规则版本，按补偿实际履约时生效的版本计算并锁定到该笔补偿履约记录；补偿履约与原履约分别记录，原核销和原结算不回改」；
--   台账 02｜Q15；CG 第 39 条「新增补偿履约由决定发放补偿的域主管法律主体承担，按补偿实际履约时生效价与提供方结算，原履约账不回改」。
-- 规则：完整补发的每项权益每次实际履约（核销成功）一行；锁定履约时目录生效价（settlement_price_cent）与目录行版本
--   （price_catalog_entry_id＝internal_price_catalog.id；无目录引用的权益两列为空、不形成应付）；
--   价格 > 0 且提供方商户的法律主体不是承担主体时，承担主体对提供方追加一条补偿应付（payable_item_id，source_type=compensation_fulfillment）；
--   该次核销被冲正时按 V344 原行不动、追加严格取反的冲正行（reversal_item_id），记冲正时点。
CREATE TABLE amusement_compensation_fulfillments (
    id                     varchar(64)  PRIMARY KEY,
    item_id                varchar(64)  NOT NULL REFERENCES amusement_fault_compensation_items(id),
    batch_id               varchar(64)  NOT NULL REFERENCES amusement_fault_compensation_batches(id),
    entitlement_id         varchar(64)  NOT NULL REFERENCES issued_entitlements(id),
    verification_id        varchar(64)  NOT NULL UNIQUE REFERENCES ticket_verifications(id),
    project_id             varchar(64)  NOT NULL,
    bearer_company_id      varchar(64)  NOT NULL,
    provider_merchant_id   varchar(64),
    settlement_price_cent  bigint       CHECK (settlement_price_cent >= 0),
    price_catalog_entry_id varchar(80)  REFERENCES internal_price_catalog(id),
    payable_item_id        varchar(128) REFERENCES ar_ap_items(id),
    fulfilled_at           timestamptz  NOT NULL,
    reversed_at            timestamptz,
    reversal_item_id       varchar(128) REFERENCES ar_ap_items(id)
);
CREATE INDEX idx_compensation_fulfillments_item ON amusement_compensation_fulfillments(item_id);

GRANT SELECT, INSERT, UPDATE ON amusement_compensation_fulfillments TO complex_app;
