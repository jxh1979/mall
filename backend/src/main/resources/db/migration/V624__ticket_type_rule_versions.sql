-- 文件版本：V0.01
-- V624（第十八批 AJ 段第 236 条 236-R3，第十八批裁决 52 ②、裁决 61）：跨域组合通票票种规则版本与审批权限。
-- 依据：ENUM:32「跨域组合通票的定价与域间结算规则由项目之上的平台权限统一审批」；BSM:754「每次发布时把凑整差额金额及各域分摊数额记入票种规则版本、
--   发布后不得再改」；第十八批裁决 61 所选选项「改动另存一份草稿版审批，审批期间照旧版卖，获批后自动换成新版」。
-- 236-R4（BSM:756「订单保存基础价、动态价、全部命中资格价、最终选中价和规则版本。」）：ticket_orders 加 ticket_type_rule_version_id，记下单时该票种生效的版本。
-- 现表（ticket_types、ticket_type_entitlements、ticket_type_gift_floors）即当前生效版；本表存每一版的快照，草稿版与待审版只存于此。
-- 每票种至多 1 份生效（effective）＋1 份未结（draft、pending_review），由两条部分唯一索引保证；effective 起内容列不再改，只改 status、decided_at。
-- project_id 供复核单项目映射（提交时票种恰属一个项目才填，同 ticket_reward_config_versions.project_id）。

CREATE TABLE ticket_type_rule_versions (
    id VARCHAR(64) PRIMARY KEY,
    ticket_type_id VARCHAR(64) NOT NULL REFERENCES ticket_types(id),
    project_id VARCHAR(64),
    version_no INTEGER NOT NULL,
    status VARCHAR(32) NOT NULL,
    weekday_price_cent BIGINT,
    weekend_price_cent BIGINT,
    holiday_price_cent BIGINT,
    combo_discount_rate NUMERIC(5,2) NOT NULL,
    min_gift_cups INTEGER,
    min_gift_cups_lower INTEGER,
    min_gift_cups_upper INTEGER,
    entitlements JSONB NOT NULL,
    component_total_cent BIGINT NOT NULL,
    computed_price_cent BIGINT NOT NULL,
    rounding_diff_cent BIGINT,
    domain_allocations JSONB NOT NULL DEFAULT '[]'::jsonb,
    reason TEXT,
    review_id VARCHAR(64),
    created_by VARCHAR(128),
    updated_by VARCHAR(128),
    submitted_by VARCHAR(128),
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,
    submitted_at TIMESTAMPTZ,
    decided_at TIMESTAMPTZ,
    CONSTRAINT uq_ttrv_ticket_type_version UNIQUE (ticket_type_id, version_no),
    CONSTRAINT chk_ttrv_status CHECK (status IN ('draft', 'pending_review', 'effective', 'superseded', 'withdrawn')),
    CONSTRAINT chk_ttrv_min_gift_cups CHECK (
        (min_gift_cups IS NULL AND min_gift_cups_lower IS NULL AND min_gift_cups_upper IS NULL)
        OR (min_gift_cups IS NOT NULL AND min_gift_cups_lower IS NOT NULL AND min_gift_cups_upper IS NOT NULL
            AND 1 <= min_gift_cups_lower AND min_gift_cups_lower <= min_gift_cups AND min_gift_cups <= min_gift_cups_upper))
);

CREATE UNIQUE INDEX uq_ttrv_one_effective ON ticket_type_rule_versions (ticket_type_id) WHERE status = 'effective';
CREATE UNIQUE INDEX uq_ttrv_one_open ON ticket_type_rule_versions (ticket_type_id) WHERE status IN ('draft', 'pending_review');

COMMENT ON TABLE ticket_type_rule_versions IS '跨域组合通票票种规则版本（ENUM:32、BSM:754）；生效版与现表一致，草稿版与待审版只存本表';
COMMENT ON COLUMN ticket_type_rule_versions.project_id IS '提交时票种所属项目（恰属一个项目才填），供复核单项目映射';
COMMENT ON COLUMN ticket_type_rule_versions.version_no IS '该票种第几版，从 1 起';
COMMENT ON COLUMN ticket_type_rule_versions.status IS 'draft 草稿、pending_review 待审批、effective 生效中、superseded 已被取代、withdrawn 已撤回；effective 起内容列不再改';
COMMENT ON COLUMN ticket_type_rule_versions.weekday_price_cent IS '平日价（分），即填的售价';
COMMENT ON COLUMN ticket_type_rule_versions.weekend_price_cent IS '周末价（分）';
COMMENT ON COLUMN ticket_type_rule_versions.holiday_price_cent IS '节假日价（分）';
COMMENT ON COLUMN ticket_type_rule_versions.combo_discount_rate IS '票种折扣（百分数，100＝不打折）';
COMMENT ON COLUMN ticket_type_rule_versions.min_gift_cups IS '最低赠酒杯数；与上下限三者同空＝没配过、按出厂 1 杯';
COMMENT ON COLUMN ticket_type_rule_versions.min_gift_cups_lower IS '最低赠酒杯数下限';
COMMENT ON COLUMN ticket_type_rule_versions.min_gift_cups_upper IS '最低赠酒杯数上限';
COMMENT ON COLUMN ticket_type_rule_versions.entitlements IS '权益组成：每条绑定的模板、提供方、业态、经营域、份数、次数、人数、各业态价格、出资方与绑定时间';
COMMENT ON COLUMN ticket_type_rule_versions.component_total_cent IS '各业态价格合计（分）';
COMMENT ON COLUMN ticket_type_rule_versions.computed_price_cent IS '系统算出售价＝合计×折扣（分）';
COMMENT ON COLUMN ticket_type_rule_versions.rounding_diff_cent IS '凑整差额＝填的平日价－算出售价（分）；平日价未填为空';
COMMENT ON COLUMN ticket_type_rule_versions.domain_allocations IS '各域分摊：各域业态价格合计、折扣分摊、凑整分摊与按填的平日价所得（BSM:754）';
COMMENT ON COLUMN ticket_type_rule_versions.reason IS '发布理由';
COMMENT ON COLUMN ticket_type_rule_versions.review_id IS '主管复核单编号；驳回后保留，页面据此显示上次被驳回';
COMMENT ON COLUMN ticket_type_rule_versions.submitted_by IS '提交审批人';
COMMENT ON COLUMN ticket_type_rule_versions.submitted_at IS '提交审批时间';
COMMENT ON COLUMN ticket_type_rule_versions.decided_at IS '获批生效时间';

ALTER TABLE ticket_orders ADD COLUMN ticket_type_rule_version_id VARCHAR(64) REFERENCES ticket_type_rule_versions(id);
COMMENT ON COLUMN ticket_orders.ticket_type_rule_version_id IS '下单时该票种生效的票种规则版本（BSM:756）；没有版本的票种为空，出票、置已付款不改写';

INSERT INTO permissions(permission_code, permission_name, domain, risk_level, requires_approval, status) VALUES
    ('ticket.product.publish.approve', '跨域组合通票票种规则审批', 'ticketing', 'high', true, 'active');

DO $$
BEGIN
    IF EXISTS (SELECT 1 FROM pg_roles WHERE rolname = 'complex_app') THEN
        EXECUTE 'GRANT SELECT, INSERT, UPDATE ON ticket_type_rule_versions TO complex_app';
    END IF;
END $$;
