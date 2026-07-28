-- V373 通票分账瀑布重建 S6:pass_gift_cost_configs——赠送权益预扣两模式配置表。
--
-- 【业务依据】business-system-map-for-operations §「通票收入分账全流程」(:739):
--   「预扣金额按票种配置——按件计＝票面各权益数量 × 内部结算价之和,按票计＝该票种赠送部分的售价整额」
--   「上述所有金额、比例、封顶、档位、期限均须后台可配,禁止写死」
--   命名不用 drink_*:新口径已不止酒水(酒水、小食、餐饮、零售等都是独立赠送权益)。
--
-- 【范式】照结算配置中心 Path 2(V326/V329):写面强制落草案(is_draft=TRUE)→ 差人复核转正(软替换旧生效行)
--   → 读侧只认 is_draft=FALSE;软删铁律(status→inactive + archive_reason,留库不物理删)。
--
-- 【维度】项目 × 票种(用户已拍板):project_id NULL=平台默认(__DEFAULT__ 归一),
--   ticket_type_id NULL=该项目所有票种通用(__ANY__ 归一)。

CREATE TABLE IF NOT EXISTS pass_gift_cost_configs (
    id                      VARCHAR(48) PRIMARY KEY,
    project_id              VARCHAR(64),                       -- NULL=平台默认;非空=某项目公司特例
    ticket_type_id          VARCHAR(64),                       -- NULL=该项目所有票种通用;非空=限某票种
    prededuct_mode          VARCHAR(24) NOT NULL DEFAULT 'by_ticket_amount',
    ticket_gift_amount_cent BIGINT,                            -- 仅按票计用:该票种赠送部分售价整额(分)
    monthly_cap_amount_cent BIGINT,                            -- 可选每月封顶(分);NULL=不封顶
    variance_handling       VARCHAR(24) NOT NULL DEFAULT 'platform_absorb',  -- 差额处理规则(S7 消费;S6 只落快照)
    status                  VARCHAR(16) NOT NULL DEFAULT 'active',
    is_draft                BOOLEAN     NOT NULL DEFAULT TRUE,
    archive_reason          TEXT,
    updated_by              VARCHAR(64),
    updated_at              TIMESTAMPTZ NOT NULL DEFAULT now(),
    -- 两模式二选一的口径锁进 DB:按件计完全由票面推导、不许带整额(带了也没人读=必然口径漂);
    -- 按票计必须给整额且 >=0(0=合法:预扣关闭态,即上线保值种子)。
    CONSTRAINT chk_pgc_mode   CHECK (prededuct_mode IN ('by_item', 'by_ticket_amount')),
    CONSTRAINT chk_pgc_amount CHECK (
        (prededuct_mode = 'by_item' AND ticket_gift_amount_cent IS NULL)
        OR (prededuct_mode = 'by_ticket_amount' AND ticket_gift_amount_cent IS NOT NULL AND ticket_gift_amount_cent >= 0)),
    CONSTRAINT chk_pgc_cap      CHECK (monthly_cap_amount_cent IS NULL OR monthly_cap_amount_cent >= 0),
    CONSTRAINT chk_pgc_variance CHECK (variance_handling IN ('platform_absorb', 'carry_forward', 'true_up_pool')),
    CONSTRAINT chk_pgc_status   CHECK (status IN ('active', 'inactive')),
    -- 坏票种 id 在写入面先拦成中文 400(照 S2/V365 综合服务费的 ticketTypeExists 校验),FK 是最后防线。
    CONSTRAINT fk_pgc_ticket_type FOREIGN KEY (ticket_type_id) REFERENCES ticket_types(id)
);

-- 草案共存唯一索引(照 V329 范式):同 scope 各一个「生效槽」+「草案槽」,编辑期不动生效行。
CREATE UNIQUE INDEX IF NOT EXISTS uq_pgc ON pass_gift_cost_configs (
    COALESCE(project_id, '__DEFAULT__'),
    COALESCE(ticket_type_id, '__ANY__'),
    is_draft
) WHERE status = 'active';

CREATE INDEX IF NOT EXISTS idx_pgc_project ON pass_gift_cost_configs (project_id, ticket_type_id);

COMMENT ON TABLE pass_gift_cost_configs IS
    'S6 赠送权益预扣配置(项目 × 票种):by_item=按件计(票面平台出资型赠送权益 数量×内部结算价 之和,无需配金额);'
    'by_ticket_amount=按票计(该票种赠送部分售价整额)。G 归娱乐平台公司,核销首笔一次性计提、冲正取反';
COMMENT ON COLUMN pass_gift_cost_configs.ticket_gift_amount_cent IS
    '按票计模式:该票种赠送部分的售价整额(分,豪华票场景:加价 59 元整块预扣即 5900)。按件计模式必须为 NULL';
COMMENT ON COLUMN pass_gift_cost_configs.variance_handling IS
    'S7 差额处理规则快照:platform_absorb=差额留娱乐平台公司仅留痕(默认,HTML 口径)/carry_forward=结转下期/true_up_pool=回补当期池';

-- ==========================================================================
-- 种子:全局默认 = 按票计 + 整额 0(R-6 拍板)
-- ==========================================================================
-- 为什么不是 by_item:按件计的默认种子不会让 G 恒 0(票面有平台出资赠品就有成本),上线瞬间就会动钱。
-- by_ticket_amount + 0 → 每张票预扣 0 → 全链逐分不变;非零由运营配置动作显式开启(先落草案、差人复核转正)。
-- 种子必须直接生效(is_draft=FALSE):读侧 requireConfigured fail-loud,无生效行会让全部核销结算响亮失败。
INSERT INTO pass_gift_cost_configs
    (id, project_id, ticket_type_id, prededuct_mode, ticket_gift_amount_cent, variance_handling,
     status, is_draft, updated_by)
VALUES ('pgc_default', NULL, NULL, 'by_ticket_amount', 0, 'platform_absorb', 'active', FALSE, 'migration:V373')
ON CONFLICT (id) DO NOTHING;

-- 生产受限运行时权限(V296/V298/V338 教训:新表必须显式 GRANT 给 complex_app,否则生产静默 permission denied 500)。
GRANT SELECT, INSERT, UPDATE, DELETE ON pass_gift_cost_configs TO complex_app;
