-- 文件版本：V0.01
-- V556：酒吧通票逐张分账——参与家数档位表与第一段名单的版本化、售票即固化可分配池、逐份分账结果（第十八批第 2 条）。
-- 依据：ENUM L52「档位为动态百分比，不是相对权重、不做归一化……按本张票的第二段参与家数查「参与家数档位表」……
--   排名一律在本张票的第二段参与者内部编列……池金额在售票时随订单固化……档位表变更须提前向受影响的酒吧商户发出通告，
--   自通告送达之日起经过通告期后方可生效；通告期为业务参数，后台可配，出厂默认一个月，须同时配置上下限……
--   第一段的参与酒吧名单，以及第一段的固定金额或固定单价，其变更……同样须提前……通告……通告期与档位表变更同一参数」；
--   操作员裁决（第十八批第 1 条）「复核转正当日系统逐家发出通告就算送达，生效日＝转正日＋通告期」。
-- 铁律 8：不回填存量——旧单梯子 ticket_distribution_rank_tiers 与月度口径 ticket_distribution_merchant_rates 自此停读（不换算）；
--   本迁移之前售出的票没有池快照。

-- 1) 分账方案版本：一套参与家数档位表＋第一段名单，草案 → 差人复核转正（当日发通告）→ 生效日起对新售出的票适用。
--    draft＝草案；notified＝已复核转正并已通告，effective_at 起生效（同一范围 effective_at 最晚且已到的一版即现行版）。
CREATE TABLE ticket_distribution_scheme_versions (
    id           VARCHAR(64) PRIMARY KEY,
    project_id   VARCHAR(64),
    status       VARCHAR(16) NOT NULL,
    drafted_by   VARCHAR(64),
    approved_by  VARCHAR(64),
    notified_at  TIMESTAMPTZ,
    effective_at TIMESTAMPTZ,
    created_at   TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT chk_tdsv_status CHECK (status IN ('draft', 'notified')),
    CONSTRAINT chk_tdsv_notified CHECK (status = 'draft' OR (notified_at IS NOT NULL AND effective_at IS NOT NULL))
);
CREATE INDEX idx_tdsv_effective ON ticket_distribution_scheme_versions (project_id, effective_at) WHERE status = 'notified';
COMMENT ON TABLE ticket_distribution_scheme_versions IS '通票分账方案版本：参与家数档位表＋第一段名单同一套通告—生效—版本机制（ENUM L52）';

-- 2) 参与家数档位表：按第二段参与家数逐行，行内各名次比例（万分比）合计必须 10000；不设家数上限；每格须同时配置上下限。
CREATE TABLE ticket_distribution_participant_tiers (
    version_id        VARCHAR(64) NOT NULL REFERENCES ticket_distribution_scheme_versions(id) ON DELETE CASCADE,
    participant_count INTEGER NOT NULL,
    rank_no           INTEGER NOT NULL,
    share_bps         INTEGER NOT NULL,
    min_bps           INTEGER,
    max_bps           INTEGER,
    PRIMARY KEY (version_id, participant_count, rank_no),
    CONSTRAINT chk_tdpt_rank CHECK (participant_count >= 1 AND rank_no BETWEEN 1 AND participant_count),
    CONSTRAINT chk_tdpt_share CHECK (share_bps BETWEEN 0 AND 10000),
    CONSTRAINT chk_tdpt_bounds CHECK ((min_bps IS NULL) = (max_bps IS NULL)
        AND (min_bps IS NULL OR (min_bps <= share_bps AND share_bps <= max_bps)))
);

-- 3) 第一段名单：酒吧 × 固定金额或固定单价二选一（照 V374 chk_tdmr_rule）；金额按每份票（每人每次入场一份）计。
CREATE TABLE ticket_distribution_first_segment_rows (
    version_id        VARCHAR(64) NOT NULL REFERENCES ticket_distribution_scheme_versions(id) ON DELETE CASCADE,
    merchant_id       VARCHAR(64) NOT NULL,
    rule_type         VARCHAR(16) NOT NULL,
    per_guest_cent    BIGINT,
    fixed_amount_cent BIGINT,
    PRIMARY KEY (version_id, merchant_id),
    CONSTRAINT chk_tdfs_rule CHECK (
        (rule_type = 'per_guest' AND per_guest_cent >= 0 AND fixed_amount_cent IS NULL)
        OR (rule_type = 'fixed' AND fixed_amount_cent >= 0 AND per_guest_cent IS NULL))
);

-- 4) 通告期（档位表与第一段变更同一参数）：出厂一个月；上下限不自拟出厂数值，首次配置须同时填写（范式同 V508）。
CREATE TABLE ticket_distribution_notice_period (
    id           VARCHAR(16) PRIMARY KEY DEFAULT 'default',
    period_value INTEGER NOT NULL,
    period_unit  VARCHAR(8) NOT NULL,
    min_value    INTEGER,
    max_value    INTEGER,
    updated_by   VARCHAR(64),
    updated_at   TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT chk_tdnp_singleton CHECK (id = 'default'),
    CONSTRAINT chk_tdnp_unit CHECK (period_unit IN ('day', 'month')),
    CONSTRAINT chk_tdnp_bounds CHECK ((min_value IS NULL) = (max_value IS NULL)
        AND (min_value IS NULL OR (1 <= min_value AND min_value <= period_value AND period_value <= max_value)))
);
INSERT INTO ticket_distribution_notice_period (id, period_value, period_unit) VALUES ('default', 1, 'month');

-- 出厂方案：ENUM L52 十行逐格（上下限留空），第一段名单为空；出厂即生效，不经通告。
INSERT INTO ticket_distribution_scheme_versions (id, project_id, status, notified_at, effective_at)
VALUES ('tdsv_factory', NULL, 'notified', TIMESTAMPTZ '2000-01-01 00:00:00+00', TIMESTAMPTZ '2000-01-01 00:00:00+00');
INSERT INTO ticket_distribution_participant_tiers (version_id, participant_count, rank_no, share_bps)
SELECT 'tdsv_factory', t.n, r.rank_no, t.bps[r.rank_no]
  FROM (VALUES
        (1,  ARRAY[10000]),
        (2,  ARRAY[5700, 4300]),
        (3,  ARRAY[4000, 3000, 3000]),
        (4,  ARRAY[3100, 2300, 2300, 2300]),
        (5,  ARRAY[2500, 2000, 2000, 2000, 1500]),
        (6,  ARRAY[2300, 1700, 1700, 1700, 1300, 1300]),
        (7,  ARRAY[2200, 1500, 1500, 1500, 1100, 1100, 1100]),
        (8,  ARRAY[1800, 1400, 1400, 1400, 1000, 1000, 1000, 1000]),
        (9,  ARRAY[1500, 1300, 1300, 1300, 1000, 1000, 1000, 1000, 600]),
        (10, ARRAY[1600, 1200, 1200, 1200, 900, 900, 900, 900, 600, 600])
       ) AS t(n, bps)
 CROSS JOIN LATERAL generate_subscripts(t.bps, 1) AS r(rank_no);

-- 5) 售票时逐张固化的酒吧通票可分配池（ENUM L52 第一至第四步）：池＝酒吧份额−通路费−销售渠道费−赠送权益预估成本−门票综合服务费。
--    slots × visits＝份数（BSM L778/L780/L800 每人每次入场一份完整票权益，按一张票计）；整张退款即作废。
CREATE TABLE pass_pool_snapshots (
    ticket_instance_id          VARCHAR(64) PRIMARY KEY,
    order_id                    VARCHAR(64) NOT NULL,
    project_id                  VARCHAR(64),
    ticket_type_id              VARCHAR(64),
    slots                       INTEGER NOT NULL,
    visits                      INTEGER NOT NULL,
    bar_gross_cent              BIGINT NOT NULL,
    platform_borne_cent         BIGINT NOT NULL DEFAULT 0,
    route_fee_cent              BIGINT NOT NULL,
    sales_channel_fee_cent      BIGINT NOT NULL,
    sales_channel_fee_config_id VARCHAR(48),
    gift_cost_cent              BIGINT NOT NULL,
    gift_cost_config_id         VARCHAR(48),
    service_fee_cent            BIGINT NOT NULL,
    service_fee_config_id       VARCHAR(48),
    pool_cent                   BIGINT NOT NULL,
    scheme_version_id           VARCHAR(64) NOT NULL REFERENCES ticket_distribution_scheme_versions(id),
    covered_bar_ids             TEXT[] NOT NULL,
    status                      VARCHAR(8) NOT NULL DEFAULT 'active',
    created_at                  TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT chk_pps_units CHECK (slots >= 1 AND visits >= 1),
    CONSTRAINT chk_pps_pool CHECK (pool_cent = bar_gross_cent - route_fee_cent - sales_channel_fee_cent
        - gift_cost_cent - service_fee_cent),
    CONSTRAINT chk_pps_status CHECK (status IN ('active', 'void'))
);
CREATE INDEX idx_pps_order ON pass_pool_snapshots (order_id);
COMMENT ON TABLE pass_pool_snapshots IS '酒吧通票可分配池售票时逐张固化（ENUM L52「池金额在售票时随订单固化」）';

-- 售票时生效方案里属于本票覆盖酒吧的第一段名单与每份金额（「已售出的票一律按售票时固化的第一段名单与固定额结算，不追溯」）。
CREATE TABLE pass_pool_snapshot_first_segment (
    ticket_instance_id VARCHAR(64) NOT NULL REFERENCES pass_pool_snapshots(ticket_instance_id) ON DELETE CASCADE,
    merchant_id        VARCHAR(64) NOT NULL,
    rule_type          VARCHAR(16) NOT NULL,
    amount_cent        BIGINT NOT NULL,
    PRIMARY KEY (ticket_instance_id, merchant_id),
    CONSTRAINT chk_ppsf_rule CHECK (rule_type IN ('per_guest', 'fixed') AND amount_cent >= 0)
);

-- 6) 逐份分账结果：每份（父票 × 人份 × 第几次入场）每家覆盖酒吧一行；已核销拿、未核销减除（ENUM L49「通票未核销减除额」）；
--    缺该家数档位行的第二段暂挂（suspended），补配生效后在后续结算单续算（resumed）。
CREATE TABLE pass_pool_unit_results (
    bill_id            VARCHAR(64) NOT NULL REFERENCES settlement_bills(id) ON DELETE CASCADE,
    ticket_instance_id VARCHAR(64) NOT NULL,
    slot_index         INTEGER NOT NULL,
    visit_no           INTEGER NOT NULL,
    merchant_id        VARCHAR(64) NOT NULL,
    segment            SMALLINT NOT NULL,
    entry_people       INTEGER,
    rank_no            INTEGER,
    pct                NUMERIC(12, 8),
    segment_pool_cent  BIGINT,
    amount_cent        BIGINT NOT NULL,
    verified           BOOLEAN NOT NULL,
    status             VARCHAR(12) NOT NULL,
    scheme_version_id  VARCHAR(64) NOT NULL,
    PRIMARY KEY (bill_id, ticket_instance_id, slot_index, visit_no, merchant_id),
    CONSTRAINT chk_ppur_segment CHECK (segment IN (1, 2)),
    CONSTRAINT chk_ppur_status CHECK (status IN ('settled', 'suspended', 'resumed'))
);
CREATE INDEX idx_ppur_unit ON pass_pool_unit_results (ticket_instance_id, slot_index, visit_no)
    WHERE status IN ('suspended', 'resumed');

-- 7) 结算单三栏分列（ENUM L52「三者即使同归一个主体，也必须分三栏分别列示」）＋暂挂＋第一段超池额（越池判定沿用现逻辑）。
ALTER TABLE settlement_bills
    ADD COLUMN unverified_deduction_cent BIGINT NOT NULL DEFAULT 0,
    ADD COLUMN unverified_deduction_entity_id VARCHAR(64),
    ADD COLUMN pool_suspended_cent BIGINT NOT NULL DEFAULT 0,
    ADD COLUMN pool_overflow_cent BIGINT NOT NULL DEFAULT 0;

-- 8) 方案变更通告短信的域与事件开关（照 V177；PGM L742 缺行即关，须显式开启；总开关 sms_notification 不动）。
INSERT INTO terminal_capability_switches
    (id, terminal_type, capability_code, scope_type, scope_id, enabled, reason, created_at, updated_at)
VALUES
    ('tcs_sms_notify_settlement', '*', 'sms.notify.settlement', 'global', 'global', TRUE,
     '结算短信域开关', now(), now()),
    ('tcs_sms_notify_settlement_distribution_scheme_notice', '*', 'sms.notify.settlement.distribution_scheme_notice',
     'global', 'global', TRUE, '通票分账方案变更通告短信事件开关', now(), now())
ON CONFLICT (terminal_type, capability_code, scope_type, scope_id) DO NOTHING;

-- 9) 商户专区通告（18B 复核后操作员裁决第 10 条「商户专区显示通告」）：方案复核转正当日逐家挂进受影响酒吧的商户专区，
--    专区可查看即送达；短信只是提醒，发不发、成不成功都不影响送达与生效。内容只写本店可见的信息
--    （ENUM L52「仅其他酒吧分得的具体金额不对其开放」）。source_id＝引发通告的方案版本。
CREATE TABLE merchant_zone_notices (
    id             VARCHAR(64) PRIMARY KEY,
    merchant_id    VARCHAR(64) NOT NULL,
    source_id      VARCHAR(64) NOT NULL,
    title          VARCHAR(200) NOT NULL,
    content        TEXT NOT NULL,
    effective_date DATE NOT NULL,
    created_at     TIMESTAMPTZ NOT NULL DEFAULT now()
);
CREATE INDEX idx_mzn_merchant ON merchant_zone_notices (merchant_id, created_at DESC);
COMMENT ON TABLE merchant_zone_notices IS '商户专区通告：挂出即送达（18B 裁决第 10 条），商户只读本商户';

DO $$
BEGIN
    IF EXISTS (SELECT 1 FROM pg_roles WHERE rolname = 'complex_app') THEN
        EXECUTE 'GRANT SELECT, INSERT, UPDATE, DELETE ON ticket_distribution_scheme_versions, '
             || 'ticket_distribution_participant_tiers, ticket_distribution_first_segment_rows, '
             || 'ticket_distribution_notice_period, pass_pool_snapshots, pass_pool_snapshot_first_segment, '
             || 'pass_pool_unit_results, merchant_zone_notices TO complex_app';
    END IF;
END $$;
