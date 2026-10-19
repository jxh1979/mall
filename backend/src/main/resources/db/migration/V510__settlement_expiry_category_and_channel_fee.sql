-- 文件版本：V0.01
-- V510：FS-023 到期未核销款分类立项、只扣一次实际渠道费并留所用配置。
-- 依据：ENUM L48「本条只适用于从未完成检票环节的票或人份……仅扣实际发生的通路费，经第三方渠道售出的再扣实际发生的
--   销售渠道费……各项均只扣一次」；ENUM L49「两类各自单独立项留痕、不合并」；ENUM L51「每笔分摊的结果与所用合同版本
--   必须留痕」；PGM L302 尾差单独记账留痕，由售票收款主体承担，跨域组合通票由娱乐平台公司承担。
-- unverified_category：never_checked_in＝从未完成检票环节（含多次票未开启的剩余次数）；session_ended＝已检票、场次终结未核销。
ALTER TABLE settlement_transactions
    ADD COLUMN unverified_category            VARCHAR(24),
    ADD COLUMN channel_fee_cent               BIGINT NOT NULL DEFAULT 0,
    ADD COLUMN channel_fee_config_id          VARCHAR(48),
    ADD COLUMN channel_fee_residual_cent      BIGINT NOT NULL DEFAULT 0,
    ADD COLUMN channel_fee_residual_entity_id VARCHAR(64),
    ADD CONSTRAINT chk_st_unverified_category
        CHECK (unverified_category IS NULL OR unverified_category IN ('never_checked_in', 'session_ended'));
