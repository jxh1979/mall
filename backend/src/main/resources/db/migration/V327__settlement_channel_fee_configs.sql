-- V327 结算配置中心批1(通票分账)-1:第三方渠道抽成率可配 —— 消灭 TicketDistributionSettlementService 写死 channelFee=0。
--   口径:结算重构 PRD §5.0 步骤② 先扣第三方渠道抽成。范式照 V223 结算批六表(项目作用域 + 生效期 + is_draft + 万分比)。
--   ⚠️ 保值:默认率 0 → 迁移前后分账生效值不变(现无第三方渠道交易额数据源=批6,率×0=0);本迁移仅建表+种子,不改逻辑,可独立回滚。
--   渠道维度(channel)预留给批6 的 per-channel 费率(如美团/抖音各不同);批1 只用通用率(channel IS NULL)。
--   新表种子直接 is_draft=FALSE(生效态)——读取侧统一强制 is_draft=FALSE,不再走 V326 那种「插草案再一次性转正」。

CREATE TABLE IF NOT EXISTS channel_fee_configs (
    id             VARCHAR(48) PRIMARY KEY,
    project_id     VARCHAR(64),                       -- NULL=全局默认;非空=某项目公司特例
    channel        VARCHAR(64),                       -- NULL=通用(所有渠道);非空=某渠道(meituan/douyin…,批6 用)
    fee_bps        INT         NOT NULL DEFAULT 0,    -- 渠道抽成万分比(0=不抽,可配)
    effective_from DATE,
    effective_to   DATE,
    status         VARCHAR(16) NOT NULL DEFAULT 'active',
    is_draft       BOOLEAN     NOT NULL DEFAULT FALSE,
    updated_by     VARCHAR(64),
    updated_at     TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT chk_cfc_bps    CHECK (fee_bps >= 0 AND fee_bps <= 10000),
    CONSTRAINT chk_cfc_status CHECK (status IN ('active', 'inactive'))
);
CREATE UNIQUE INDEX IF NOT EXISTS uq_cfc
    ON channel_fee_configs (COALESCE(project_id, '__DEFAULT__'), COALESCE(channel, '__ALL__'));

-- 种子:全局默认通用渠道费率 0(生效态)= 消灭写死常量、保持现值 0(零行为变更)。
INSERT INTO channel_fee_configs (id, project_id, channel, fee_bps, is_draft)
VALUES ('cfc_default', NULL, NULL, 0, FALSE)
ON CONFLICT (id) DO NOTHING;
