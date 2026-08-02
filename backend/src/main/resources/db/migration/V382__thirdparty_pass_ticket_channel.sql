-- V382 批6 第三方通票适配：渠道票种映射表 + integration 域 business_type 扩 'ticket' + 回传队列反查索引。
-- HTML 依据：「美团、大众点评、抖音首批只做团购购买、订单回流、核销和对账」；
--   「通票售卖主体：综合体通票只能由项目公司售卖（官方 App/小程序或授权的第三方渠道）」。
-- 开关口径：V071 已种 tcs_int_ticket_chan_off（integration.ticket_channel.sync 全局 FALSE=默认关），
--   本迁移不动开关——开关关时 ticket 回流在服务层拒收（渠道未启用），设计性 gating 与既有范式一致。

-- ── 1) 渠道票种映射：第三方商品 id → 本地票种（范式照 V334 integration_channel_store_map）──────
-- 招商/运营后台录入；price_rule_id 可空=按票种门市价，非空=按渠道团购价规则出票（本地订单金额
-- 与渠道实收对齐，自动对账才对得上）。status 软删；自然键幂等 upsert（重录改绑/复活不长重复行）。
CREATE TABLE IF NOT EXISTS integration_channel_ticket_type_map (
    id                  VARCHAR(64)  PRIMARY KEY,
    provider_code       VARCHAR(128) NOT NULL,
    channel             VARCHAR(32)  NOT NULL,
    external_product_id VARCHAR(160) NOT NULL,
    ticket_type_id      VARCHAR(64)  NOT NULL REFERENCES ticket_types(id),
    price_rule_id       VARCHAR(64)  REFERENCES ticket_price_rules(id),
    status              VARCHAR(16)  NOT NULL DEFAULT 'active',
    created_at          TIMESTAMPTZ  NOT NULL,
    updated_at          TIMESTAMPTZ  NOT NULL,
    CONSTRAINT uq_ictt_map UNIQUE (provider_code, channel, external_product_id),
    CONSTRAINT chk_ictt_map_channel CHECK (channel IN (
        'meituan', 'dianping', 'douyin', 'koubei', 'eleme', 'alipay', 'wechat', 'internal', 'unknown'
    )),
    CONSTRAINT chk_ictt_map_status CHECK (status IN ('active', 'inactive'))
);

CREATE INDEX IF NOT EXISTS idx_ictt_map_ticket_type
    ON integration_channel_ticket_type_map (ticket_type_id, status);

-- ── 2) integration 域五表 business_type 扩 'ticket'（V030 建表时只有 catering/retail）─────────
ALTER TABLE integration_order_syncs
    DROP CONSTRAINT IF EXISTS chk_integration_order_syncs_business;
ALTER TABLE integration_order_syncs
    ADD CONSTRAINT chk_integration_order_syncs_business CHECK (business_type IN ('catering', 'retail', 'ticket'));

ALTER TABLE integration_retry_tasks
    DROP CONSTRAINT IF EXISTS chk_integration_retry_tasks_business;
ALTER TABLE integration_retry_tasks
    ADD CONSTRAINT chk_integration_retry_tasks_business CHECK (business_type IN ('catering', 'retail', 'ticket'));

ALTER TABLE integration_compensation_tasks
    DROP CONSTRAINT IF EXISTS chk_integration_compensation_tasks_business;
ALTER TABLE integration_compensation_tasks
    ADD CONSTRAINT chk_integration_compensation_tasks_business CHECK (business_type IN ('catering', 'retail', 'ticket'));

ALTER TABLE integration_reconciliation_batches
    DROP CONSTRAINT IF EXISTS chk_integration_recon_batches_business;
ALTER TABLE integration_reconciliation_batches
    ADD CONSTRAINT chk_integration_recon_batches_business CHECK (business_type IN ('catering', 'retail', 'ticket'));

ALTER TABLE integration_reconciliation_differences
    DROP CONSTRAINT IF EXISTS chk_integration_recon_diffs_business;
ALTER TABLE integration_reconciliation_differences
    ADD CONSTRAINT chk_integration_recon_diffs_business CHECK (business_type IN ('catering', 'retail', 'ticket'));

-- ── 3) 核销回传队列反查索引：消费者按本地票订单 id 找回流行（只出票成功的 ticket 行有该键）────
CREATE INDEX IF NOT EXISTS idx_integration_order_syncs_local_order
    ON integration_order_syncs ((provider_payload_json->>'local_ticket_order_id'))
    WHERE provider_payload_json ? 'local_ticket_order_id';

-- ── 4) 权限扩义：渠道映射维护权限同管两张映射表（门店映射 V334 + 票种映射本批），
--     同一拨人（招商/运营）干同一件事（渠道映射录入），不另立权限码增加后台配置负担。──────────
UPDATE permissions
   SET permission_name = 'View channel mappings (store & ticket type)'
 WHERE permission_code = 'integration.channel_store_map.view';
UPDATE permissions
   SET permission_name = 'Maintain channel mappings (store & ticket type)'
 WHERE permission_code = 'integration.channel_store_map.manage';

-- ── 5) GRANT 受限运行时（V296/V298/V338 铁律：新表一律 GRANT complex_app 兜底）────────────────
DO $$
BEGIN
    IF EXISTS (SELECT 1 FROM pg_roles WHERE rolname = 'complex_app') THEN
        EXECUTE 'GRANT SELECT, INSERT, UPDATE, DELETE ON integration_channel_ticket_type_map TO complex_app';
    ELSE
        RAISE NOTICE 'skip GRANT to complex_app (restricted runtime role absent in this environment)';
    END IF;
END $$;
