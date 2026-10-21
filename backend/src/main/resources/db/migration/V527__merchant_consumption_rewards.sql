-- 文件版本：V0.01
-- V527：TK-04-a 商户消费（酒吧账单、专项服务单）会员识别与整笔履约完成后的待确认积分、成长值（第十一批）。
-- 依据：BSM L822「积分与成长值获取」（按商户业态可配“每消费多少元产生一积分”须同时配置上下限；积分规则与成长值规则分别配置；
--   POS、现金等由收银现场录入会员号累计，走客人查询专门通道并留痕）；BSM L823「冻结与有效消费」（整笔有效消费全部实际履约完成时点
--   统一起算等待期，全国统一配置、出厂默认 24 小时、须同时配置上下限，单据生成时锁定版本；券抵扣部分不属于有效消费）；
--   DEC-R8-93（业态积分获取率由对应域主管公司审批）；2026-10-21 操作员裁决补充第 4 条（商户消费成长值按商户业态分别配置
--   「每消费多少元记 1 成长值」，与积分获取率独立；未配置的业态不记成长值、不设出厂数值；值与上下限同配）。
-- 不回填存量数据。

-- ① 奖励规则版本扩两类，按商户业态（merchants.merchant_type 六值）全国生效：
--    merchant_points＝每产生 1 积分所需有效消费（分）；merchant_growth＝每记 1 成长值所需有效消费（分）。
ALTER TABLE ticket_reward_config_versions
    ADD COLUMN business_format VARCHAR(16)
        CHECK (business_format IN ('bar', 'catering', 'retail', 'amusement', 'media', 'other'));
ALTER TABLE ticket_reward_config_versions
    DROP CONSTRAINT ticket_reward_config_versions_kind_check,
    DROP CONSTRAINT ticket_reward_config_versions_check;
ALTER TABLE ticket_reward_config_versions
    ADD CONSTRAINT ticket_reward_config_versions_kind_check
        CHECK (kind IN ('points', 'growth', 'national', 'sales_contract', 'recovery_waiver', 'merchant_points', 'merchant_growth')),
    ADD CONSTRAINT ticket_reward_config_versions_check
        CHECK ((kind IN ('national', 'recovery_waiver') AND ticket_type_id IS NULL AND project_id IS NULL AND business_format IS NULL)
            OR (kind IN ('merchant_points', 'merchant_growth') AND ticket_type_id IS NULL AND project_id IS NULL AND business_format IS NOT NULL)
            OR (kind IN ('points', 'growth', 'sales_contract') AND ticket_type_id IS NOT NULL AND project_id IS NOT NULL AND business_format IS NULL));
DROP INDEX uq_ticket_reward_config_effective;
DROP INDEX uq_ticket_reward_config_version;
CREATE UNIQUE INDEX uq_ticket_reward_config_effective ON ticket_reward_config_versions
    (kind, COALESCE(ticket_type_id, ''), COALESCE(project_id, ''), COALESCE(business_format, ''), effective_at);
CREATE UNIQUE INDEX uq_ticket_reward_config_version ON ticket_reward_config_versions
    (kind, COALESCE(ticket_type_id, ''), COALESCE(project_id, ''), COALESCE(business_format, ''), version);

-- ② 收银现场识别的会员（录入会员手机号或储值支付短信验证的客人）；无此行时回落链上订单下单客人。
CREATE TABLE merchant_consumption_members (
    source_type VARCHAR(24) NOT NULL CHECK (source_type IN ('bar_bill', 'service_order')),
    source_id VARCHAR(64) NOT NULL,
    customer_id VARCHAR(80) NOT NULL REFERENCES customers(id),
    identified_at TIMESTAMPTZ NOT NULL,
    PRIMARY KEY (source_type, source_id)
);

-- ③ 整笔有效消费全部履约完成快照：一张单据一条；规则版本、倍率、等待期在生成时锁定。
CREATE TABLE merchant_consumption_rewards (
    id VARCHAR(64) PRIMARY KEY,
    source_type VARCHAR(24) NOT NULL CHECK (source_type IN ('bar_bill', 'service_order')),
    source_id VARCHAR(64) NOT NULL,
    customer_id VARCHAR(80) NOT NULL REFERENCES customers(id),
    merchant_id VARCHAR(64) NOT NULL REFERENCES merchants(id),
    project_id VARCHAR(64),
    business_format VARCHAR(16) NOT NULL,
    effective_amount_cent BIGINT NOT NULL CHECK (effective_amount_cent > 0),
    points_rule_id VARCHAR(64) REFERENCES ticket_reward_config_versions(id),
    growth_rule_id VARCHAR(64) REFERENCES ticket_reward_config_versions(id),
    wait_rule_id VARCHAR(64) REFERENCES ticket_reward_config_versions(id),
    level_code VARCHAR(64),
    level_multiplier_bp BIGINT NOT NULL CHECK (level_multiplier_bp > 0),
    birthday_multiplier_bp BIGINT NOT NULL CHECK (birthday_multiplier_bp > 0),
    paid_at TIMESTAMPTZ NOT NULL,
    base_points BIGINT NOT NULL CHECK (base_points >= 0),
    points BIGINT NOT NULL CHECK (points >= 0),
    growth BIGINT NOT NULL CHECK (growth >= 0),
    waiting_hours INTEGER CHECK (waiting_hours >= 0),
    completed_at TIMESTAMPTZ NOT NULL,
    available_at TIMESTAMPTZ,
    status VARCHAR(24) NOT NULL CHECK (status IN ('pending', 'confirmed', 'cancelled', 'reversed')),
    created_at TIMESTAMPTZ NOT NULL,
    UNIQUE (source_type, source_id),
    CHECK (points + growth > 0),
    CHECK (points = 0 OR points_rule_id IS NOT NULL),
    CHECK (growth = 0 OR growth_rule_id IS NOT NULL),
    CHECK ((points > 0) = (wait_rule_id IS NOT NULL AND waiting_hours IS NOT NULL AND available_at IS NOT NULL)),
    CHECK (status <> 'pending' OR points > 0)
);
CREATE INDEX idx_merchant_rewards_pending ON merchant_consumption_rewards (available_at, id) WHERE status = 'pending';
CREATE INDEX idx_merchant_rewards_customer ON merchant_consumption_rewards (customer_id, completed_at, id);

-- ④ 成长值流水同时承接门票与商户消费两类来源，二者恰有其一。
ALTER TABLE customer_growth_ledger
    ALTER COLUMN reward_id DROP NOT NULL,
    ADD COLUMN merchant_reward_id VARCHAR(64) REFERENCES merchant_consumption_rewards(id),
    ADD CONSTRAINT chk_customer_growth_ledger_source CHECK (num_nonnulls(reward_id, merchant_reward_id) = 1),
    ADD CONSTRAINT uq_customer_growth_ledger_merchant UNIQUE (merchant_reward_id, change_type);

DO $$
BEGIN
    IF EXISTS (SELECT 1 FROM pg_roles WHERE rolname = 'complex_app') THEN
        EXECUTE 'GRANT SELECT, INSERT, UPDATE, DELETE ON merchant_consumption_members TO complex_app';
        EXECUTE 'GRANT SELECT, INSERT, UPDATE, DELETE ON merchant_consumption_rewards TO complex_app';
    END IF;
END $$;
