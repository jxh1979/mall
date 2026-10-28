-- 文件版本：V0.01
-- V558：第十八批 C 段（待开发清单第 11、12 条）。
-- ① 第 11 条：储值充值赠送比例走既有涉钱配置版本与审批（BSM L521「所有涉及钱的配置一律版本化……先存为草案、经高敏权限“转正”审批通过后按指定日期生效」
--    「每一笔单据在生成时就把所用配置版本固化在单上」；docs/decisions/2026-10-18-cg-pending-decisions.md 第 11、13 条），并入奖励规则版本表：
--    merchant_card_bonus＝商户级卡赠送比例（ENUM L86「赠送比例由发卡商户提出、由平台运营代为录入后台」），按发卡商户 merchant_id，不限定项目；
--    stored_value_bonus＝综合体通用卡赠送比例（ENUM L86「赠送比例可配（业务参数，按项目可配，须同时配置上下限）」），project_id 为空＝无项目通用卡。
--    payload.fixedValue＝赠送比例（万分比），minimumValue/maximumValue＝上下限。充值流水记下所用版本。原「存即生效」的赠送比例表删除，不迁存量。
-- ② 第 12 条：会员等级档位（BSM L820/L821，ENUM L70/L72，DEC-R3-2026-08-04-35）与生日加倍规则（BSM L831，ENUM L87，DEC-R8-97-13）全平台一套，去掉 project_id。
-- 不回填存量数据。

ALTER TABLE ticket_reward_config_versions ADD COLUMN merchant_id VARCHAR(64) REFERENCES merchants(id);
ALTER TABLE ticket_reward_config_versions
    DROP CONSTRAINT ticket_reward_config_versions_kind_check,
    DROP CONSTRAINT ticket_reward_config_versions_check;
ALTER TABLE ticket_reward_config_versions
    ADD CONSTRAINT ticket_reward_config_versions_kind_check
        CHECK (kind IN ('points', 'growth', 'national', 'sales_contract', 'recovery_waiver', 'merchant_points', 'merchant_growth',
                        'level_rolling_months', 'level_protection_months', 'level_notice_days', 'merchant_card_bonus', 'stored_value_bonus')),
    ADD CONSTRAINT ticket_reward_config_versions_check
        CHECK ((kind IN ('national', 'recovery_waiver', 'level_rolling_months', 'level_protection_months', 'level_notice_days')
                AND ticket_type_id IS NULL AND project_id IS NULL AND business_format IS NULL AND merchant_id IS NULL)
            OR (kind IN ('merchant_points', 'merchant_growth') AND ticket_type_id IS NULL AND project_id IS NULL AND business_format IS NOT NULL AND merchant_id IS NULL)
            OR (kind IN ('points', 'growth', 'sales_contract') AND ticket_type_id IS NOT NULL AND project_id IS NOT NULL AND business_format IS NULL AND merchant_id IS NULL)
            OR (kind = 'merchant_card_bonus' AND ticket_type_id IS NULL AND project_id IS NULL AND business_format IS NULL AND merchant_id IS NOT NULL)
            OR (kind = 'stored_value_bonus' AND ticket_type_id IS NULL AND business_format IS NULL AND merchant_id IS NULL));
DROP INDEX uq_ticket_reward_config_effective;
DROP INDEX uq_ticket_reward_config_version;
CREATE UNIQUE INDEX uq_ticket_reward_config_effective ON ticket_reward_config_versions
    (kind, COALESCE(ticket_type_id, ''), COALESCE(project_id, ''), COALESCE(business_format, ''), COALESCE(merchant_id, ''), effective_at);
CREATE UNIQUE INDEX uq_ticket_reward_config_version ON ticket_reward_config_versions
    (kind, COALESCE(ticket_type_id, ''), COALESCE(project_id, ''), COALESCE(business_format, ''), COALESCE(merchant_id, ''), version);

ALTER TABLE customer_stored_value_transactions
    ADD COLUMN bonus_config_version_id VARCHAR(64) REFERENCES ticket_reward_config_versions(id);
COMMENT ON COLUMN customer_stored_value_transactions.bonus_config_version_id IS
    '充值时所用赠送比例版本（ticket_reward_config_versions 的 merchant_card_bonus / stored_value_bonus）；无生效版本＝不赠送、为空';

DROP TABLE stored_value_bonus_policies;

DELETE FROM customer_member_levels WHERE project_id IS NOT NULL;
ALTER TABLE customer_member_levels DROP CONSTRAINT uq_cml_project_level;
DROP INDEX idx_customer_member_levels_project_rank;
ALTER TABLE customer_member_levels DROP COLUMN project_id;
ALTER TABLE customer_member_levels ADD CONSTRAINT uq_cml_level_code UNIQUE (level_code);

DELETE FROM customer_birthday_rules WHERE project_id IS NOT NULL;
ALTER TABLE customer_birthday_rules DROP CONSTRAINT uq_cbr_project_status;
DROP INDEX idx_customer_birthday_rules_project;
ALTER TABLE customer_birthday_rules DROP COLUMN project_id;
ALTER TABLE customer_birthday_rules ADD CONSTRAINT uq_cbr_rule_name UNIQUE (rule_name);
