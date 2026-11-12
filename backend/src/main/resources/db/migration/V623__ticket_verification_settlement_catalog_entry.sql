-- 文件版本：V0.01
-- V623（第十八批 AI 段第 236 条 236-R1，第十八批裁决 52 ①内部结算价审批版本）：核销时记下所用内部结算价目录行（＝审批版本）。
-- 依据：BSM:1754「每项权益都必须绑定实际提供方和审批版本，避免悬空账。」；ENUM:54「价格变更形成新版本并只对后续生效，核销或结算时锁定当时版本。」
-- 目录转正为软替换（旧行 status→inactive 留库，V379），目录行 id 即版本，外键成立；写法同 V505 price_catalog_entry_id。

ALTER TABLE ticket_verifications
    ADD COLUMN settlement_catalog_entry_id VARCHAR(80) REFERENCES internal_price_catalog(id);

COMMENT ON COLUMN ticket_verifications.settlement_catalog_entry_id IS
    '核销所用内部结算价目录行（internal_price_catalog.id＝审批版本；ENUM:54「核销或结算时锁定当时版本」）；非提供方兑付权益与商户让利专属直付行为空';
