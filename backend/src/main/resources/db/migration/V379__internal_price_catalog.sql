-- V379 内部结算 SKU 目录（批 10/E·DEC-ORG-2026-07-28-12 + DEC-MPS-2026-07-28-15，业务 HTML「内部结算价目录」节）：
--   平台内部结算价收拢成唯一真源目录。每条条目硬绑提供商家（业态由 merchants.merchant_type 带出，不冗余存）、
--   归入品类（独立标签）；商家/业态/品类三维可筛。
-- 草案/生效并存（结算配置中心 Path 2 同款）：
--   item_code = 条目稳定业务键（业务表引用它，不引用行 id）；同一 item_code 至多一行生效（is_draft=FALSE）
--   + 一行草案（is_draft=TRUE）；转正 = 旧生效行软替换 inactive（留库即价格版本史）+ 草案翻生效。
--   改价须草案 → 他人复核转正（settlement.config.manage 高敏 + 差人守卫）→ 只对后续核销/结算生效。
-- 项目隔离（DEC-MPS-2026-07-28-15「SKU 不得跨项目关联」）：引用校验收紧为「条目提供商家 = 业务对象提供商家」
--   （商户唯一归属项目，商户级同源强于项目级同源），不单存 project_id。
-- 敏感：内部结算价仅授权后台可见（读端沿用 V279/V301 脱敏铁律，无权限一律不吐价）。
CREATE TABLE IF NOT EXISTS internal_price_catalog (
    id             VARCHAR(80) PRIMARY KEY,
    item_code      VARCHAR(80) NOT NULL,
    merchant_id    VARCHAR(64) NOT NULL REFERENCES merchants(id),
    category       VARCHAR(64) NOT NULL,
    item_name      VARCHAR(200) NOT NULL,
    -- 内部结算价（分）。0 合法 = 商户让利型（平台不出钱，S5 同一套 0/NULL 语义）；「没配价」= 无生效行，不落 NULL 行。
    price_cent     BIGINT NOT NULL,
    status         VARCHAR(16) NOT NULL DEFAULT 'active',
    is_draft       BOOLEAN NOT NULL DEFAULT TRUE,
    archive_reason TEXT,
    updated_by     VARCHAR(64),
    created_at     TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at     TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT chk_ipc_price CHECK (price_cent >= 0),
    CONSTRAINT chk_ipc_status CHECK (status IN ('active', 'inactive'))
);

-- 草案槽唯一性：同 item_code 至多一行生效 + 一行草案（历史 inactive 行不受限 = 版本留痕）。
CREATE UNIQUE INDEX IF NOT EXISTS uq_ipc_code_slot
    ON internal_price_catalog (item_code, is_draft) WHERE status = 'active';
-- 三维筛选：商家（业态经 merchants join 带出）与品类。
CREATE INDEX IF NOT EXISTS idx_ipc_merchant ON internal_price_catalog (merchant_id, status);
CREATE INDEX IF NOT EXISTS idx_ipc_category ON internal_price_catalog (category, status);

-- 生产受限运行时权限（V296/V298/V338 教训：新表必须显式 GRANT 给 complex_app，否则运行时静默 permission denied 500）。
GRANT SELECT, INSERT, UPDATE, DELETE ON internal_price_catalog TO complex_app;
