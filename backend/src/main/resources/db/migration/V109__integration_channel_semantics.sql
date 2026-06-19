-- P0#11 第三方回流渠道语义 + mapPayload + 自动对账（third-party §14.11b/§2B / R6-D02）。
-- 现状证据：
--   · V030 建 integration_order_syncs 仅 business_type(catering/retail) + sync_type，
--     无 channel / external_channel_order_id 列——无法区分美团/点评/抖音等回流渠道，
--     渠道级幂等（同一渠道单号重复回流）无约束。
--   · V030 建 integration_reconciliation_differences 无 source_domain 列——自动对账产出的
--     第三方回流差异无法标记来源域以衔接统一异常链口径。
-- 本迁移收口（R6-D02：资料未到=开关 off + mock + 映射骨架先行）：
--   1) integration_order_syncs 补 channel / external_channel_order_id 列 + 渠道级幂等唯一约束 + 检索索引；
--   2) integration_reconciliation_differences 补 source_domain 列（platform|thirdparty），
--      自动对账写 thirdparty，既有人工录入写 platform；
--   3) channel CHECK 与服务层 IntegrationValidation.CHANNELS 占位集合对齐（资料到位后再扩充）。

-- ── 1) integration_order_syncs：渠道语义列 ─────────────────────────────────────
ALTER TABLE integration_order_syncs
    ADD COLUMN IF NOT EXISTS channel VARCHAR(32) NOT NULL DEFAULT 'unknown',
    ADD COLUMN IF NOT EXISTS external_channel_order_id VARCHAR(160);

-- channel 取值约束（占位集合，与服务层 CHANNELS 对齐；资料到位后随版本扩充）。
ALTER TABLE integration_order_syncs
    DROP CONSTRAINT IF EXISTS chk_integration_order_syncs_channel;

ALTER TABLE integration_order_syncs
    ADD CONSTRAINT chk_integration_order_syncs_channel CHECK (channel IN (
        'meituan', 'dianping', 'douyin', 'koubei', 'eleme', 'alipay', 'wechat', 'internal', 'unknown'
    ));

-- 渠道级幂等：同一渠道 + 渠道外部单号唯一（external_channel_order_id 为空时不约束，允许尚未提供渠道单号的回流）。
CREATE UNIQUE INDEX IF NOT EXISTS uq_integration_order_syncs_channel_order
    ON integration_order_syncs (channel, external_channel_order_id)
    WHERE external_channel_order_id IS NOT NULL;

-- 渠道检索索引（按 provider + channel + 业务类型 + 时间取数，供自动对账比对）。
CREATE INDEX IF NOT EXISTS idx_integration_order_syncs_channel
    ON integration_order_syncs (provider_code, channel, business_type, updated_at DESC);

-- ── 2) integration_reconciliation_differences：来源域标记（衔接统一异常链口径）─────
ALTER TABLE integration_reconciliation_differences
    ADD COLUMN IF NOT EXISTS source_domain VARCHAR(16) NOT NULL DEFAULT 'platform';

ALTER TABLE integration_reconciliation_differences
    DROP CONSTRAINT IF EXISTS chk_integration_recon_diffs_source_domain;

ALTER TABLE integration_reconciliation_differences
    ADD CONSTRAINT chk_integration_recon_diffs_source_domain CHECK (source_domain IN (
        'platform', 'thirdparty'
    ));

-- 自动对账差异按来源域 + 状态检索（异常复核取 thirdparty 未结差异）。
CREATE INDEX IF NOT EXISTS idx_integration_recon_diffs_source_domain
    ON integration_reconciliation_differences (source_domain, status, updated_at DESC);
