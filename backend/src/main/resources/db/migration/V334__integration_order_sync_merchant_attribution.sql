-- P1a 第三方回流落商户 + 渠道门店号→商户映射（营业额抽成 / 店级·业态级租售比按店归属）。
-- 断点：integration_order_syncs 无 merchant_id，回流营业额拆不到店 → 抽成取数抛错、租售比退化平台级。
-- 口径（2026-07-21 用户拍板）：
--   · merchant_id 可空。NULL = 未归属（第三方给不到门店号 / 未维护映射 / 存量历史行），
--     下游只进平台级租售比、不参与任何店级抽成，报表显式标注——诚实不静默，绝不硬塞给某商户。
--   · 存量历史行 merchant_id 一律留 NULL（provider_code 非商户，无法可靠追溯）。

-- ── 1) integration_order_syncs：补可空商户列 + 部分索引（仅已归属行进索引）─────────────
ALTER TABLE integration_order_syncs
    ADD COLUMN IF NOT EXISTS merchant_id VARCHAR(64);

-- 整型 backstop（P1 资金复核）：非空 merchant_id 必须指向真实商户。落商户解析器已保证只有
-- 合法 id 或 NULL 到达 INSERT（查无此商户 → 置 NULL 落未归属桶），此 FK 仅作最后防线，
-- 挡住绕开解析器的直插脏值；NULL（未归属）被 Postgres FK 豁免，未归属行照留。
DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM pg_constraint WHERE conname = 'fk_integration_order_syncs_merchant'
    ) THEN
        ALTER TABLE integration_order_syncs
            ADD CONSTRAINT fk_integration_order_syncs_merchant
            FOREIGN KEY (merchant_id) REFERENCES merchants(id);
    END IF;
END $$;

CREATE INDEX IF NOT EXISTS idx_integration_order_syncs_merchant_created
    ON integration_order_syncs (merchant_id, created_at)
    WHERE merchant_id IS NOT NULL;

-- ── 2) 渠道门店号 → 商户映射表 ─────────────────────────────────────────────────
-- 源A：渠道 API 回流自带门店号（payload.external_store_id）→ 靠本表翻译成 merchant_id。
-- 维护入口：招商签约录第三方门店号 / 运营后台补录（口径④）。status 软删；自然键唯一，
-- 重录同一门店号走 upsert（DO UPDATE 改绑商户 / 复活），不新增重复行。
CREATE TABLE IF NOT EXISTS integration_channel_store_map (
    id                 VARCHAR(64)  PRIMARY KEY,
    provider_code      VARCHAR(128) NOT NULL,
    channel            VARCHAR(32)  NOT NULL,
    external_store_id  VARCHAR(160) NOT NULL,
    merchant_id        VARCHAR(64)  NOT NULL REFERENCES merchants(id),
    status             VARCHAR(16)  NOT NULL DEFAULT 'active',
    created_at         TIMESTAMPTZ  NOT NULL,
    updated_at         TIMESTAMPTZ  NOT NULL,
    CONSTRAINT uq_ics_map UNIQUE (provider_code, channel, external_store_id),
    CONSTRAINT chk_ics_map_channel CHECK (channel IN (
        'meituan', 'dianping', 'douyin', 'koubei', 'eleme', 'alipay', 'wechat', 'internal', 'unknown'
    )),
    CONSTRAINT chk_ics_map_status CHECK (status IN ('active', 'inactive'))
);

CREATE INDEX IF NOT EXISTS idx_ics_map_merchant
    ON integration_channel_store_map (merchant_id, status);

-- ── 3) 权限：渠道门店映射维护（配置类，medium，无审批——误配可改、只影响未来归属）──────
INSERT INTO permissions (permission_code, permission_name, domain, risk_level, requires_approval, status)
VALUES
    ('integration.channel_store_map.view', 'View channel-store to merchant mappings', 'integration', 'medium', FALSE, 'active'),
    ('integration.channel_store_map.manage', 'Maintain channel-store to merchant mappings', 'integration', 'medium', FALSE, 'active')
ON CONFLICT (permission_code) DO UPDATE SET
    permission_name = EXCLUDED.permission_name,
    domain = EXCLUDED.domain,
    risk_level = EXCLUDED.risk_level,
    requires_approval = EXCLUDED.requires_approval,
    status = EXCLUDED.status;

INSERT INTO role_permissions (role_id, permission_code)
SELECT 'role_platform_admin', permission_code
  FROM permissions
 WHERE permission_code IN (
        'integration.channel_store_map.view',
        'integration.channel_store_map.manage'
 )
ON CONFLICT (role_id, permission_code) DO NOTHING;
