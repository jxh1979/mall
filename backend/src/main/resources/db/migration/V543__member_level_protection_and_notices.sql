-- 文件版本：V0.01
-- V543：OBS-13-01 会员等级保护期、降级预警、「已保级」与降级通知（第十四批）。
-- 依据：BSM L820「等级与成长值」（滚动统计周期、保护期、提前通知天数三项业务参数全平台统一可配、出厂默认十二个月／十二个月／三十天、
--   须同时配置上下限；变更须经审批、只对后续周期生效）；ENUM L70 同义；操作员裁决补充（第十四批）第 1、2 条。
-- 不回填存量数据。

-- ① 三项等级参数并入奖励规则版本表（版本化、审批、未来生效时间），不可限定项目、票种、业态。
--    出厂只落参数值，上下限 HTML 未给出：平台首次改参时值与上下限同填并走审批。
ALTER TABLE ticket_reward_config_versions
    DROP CONSTRAINT ticket_reward_config_versions_kind_check,
    DROP CONSTRAINT ticket_reward_config_versions_check;
ALTER TABLE ticket_reward_config_versions
    ADD CONSTRAINT ticket_reward_config_versions_kind_check
        CHECK (kind IN ('points', 'growth', 'national', 'sales_contract', 'recovery_waiver', 'merchant_points', 'merchant_growth',
                        'level_rolling_months', 'level_protection_months', 'level_notice_days')),
    ADD CONSTRAINT ticket_reward_config_versions_check
        CHECK ((kind IN ('national', 'recovery_waiver', 'level_rolling_months', 'level_protection_months', 'level_notice_days')
                AND ticket_type_id IS NULL AND project_id IS NULL AND business_format IS NULL)
            OR (kind IN ('merchant_points', 'merchant_growth') AND ticket_type_id IS NULL AND project_id IS NULL AND business_format IS NOT NULL)
            OR (kind IN ('points', 'growth', 'sales_contract') AND ticket_type_id IS NOT NULL AND project_id IS NOT NULL AND business_format IS NULL));
INSERT INTO ticket_reward_config_versions (id, kind, version, payload, effective_at, status, created_by, created_at, approved_at) VALUES
    ('trc_level_rolling_months_factory', 'level_rolling_months', 'V0.01', '{"fixedValue": 12}',
     '2000-01-01T00:00:00Z', 'approved', 'system', '2000-01-01T00:00:00Z', '2000-01-01T00:00:00Z'),
    ('trc_level_protection_months_factory', 'level_protection_months', 'V0.01', '{"fixedValue": 12}',
     '2000-01-01T00:00:00Z', 'approved', 'system', '2000-01-01T00:00:00Z', '2000-01-01T00:00:00Z'),
    ('trc_level_notice_days_factory', 'level_notice_days', 'V0.01', '{"fixedValue": 30}',
     '2000-01-01T00:00:00Z', 'approved', 'system', '2000-01-01T00:00:00Z', '2000-01-01T00:00:00Z');

-- 定级按客人取滚动统计周期内的成长值流水（每日作业逐账户、成长值入账与冲回即时重算）。
CREATE INDEX idx_customer_growth_ledger_customer ON customer_growth_ledger (customer_id, created_at);

-- ② 积分账户原「宽限期截止」改为本轮保护期截止（只有升级才起保护期；level_period_start 为本轮起点＝升级时点）。
ALTER TABLE customer_points_accounts RENAME COLUMN grace_until TO protection_until;

-- ③ 等级通知（官方 App／小程序站内弹出；BSM L1596 客人短信清单不含等级通知）：
--    downgrade_warning＝降级预警（target_level＝按发出当日成长值测算将降至的等级）；level_kept＝已保级（target_level＝原等级）；
--    downgraded＝降级生效后补发（target_level＝新等级）。cycle_end＝该轮保护期截止，保护期外按滚动成长值降级时为空。
CREATE TABLE customer_level_notices (
    id VARCHAR(64) PRIMARY KEY,
    customer_id VARCHAR(80) NOT NULL REFERENCES customers(id),
    notice_type VARCHAR(24) NOT NULL CHECK (notice_type IN ('downgrade_warning', 'level_kept', 'downgraded')),
    cycle_end TIMESTAMPTZ,
    current_level VARCHAR(64) NOT NULL,
    target_level VARCHAR(64) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL,
    read_at TIMESTAMPTZ,
    CHECK (notice_type = 'downgraded' OR cycle_end IS NOT NULL)
);
-- 每轮保护期预警、已保级各至多一次。
CREATE UNIQUE INDEX uq_customer_level_notices_cycle ON customer_level_notices (customer_id, cycle_end, notice_type)
    WHERE notice_type IN ('downgrade_warning', 'level_kept');
CREATE INDEX idx_customer_level_notices_unread ON customer_level_notices (customer_id, created_at) WHERE read_at IS NULL;

DO $$
BEGIN
    IF EXISTS (SELECT 1 FROM pg_roles WHERE rolname = 'complex_app') THEN
        EXECUTE 'GRANT SELECT, INSERT, UPDATE ON customer_level_notices TO complex_app';
    END IF;
END $$;
