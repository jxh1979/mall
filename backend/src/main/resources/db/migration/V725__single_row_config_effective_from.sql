-- 文件版本：V0.01
-- V725（第十八批 BQ 段第 371 条 371-R1、371-R3）：结算配置中心其余单行涉钱配置——分配方式（含短期经营激励单次有效期上限）、门票综合服务费、
--   赠送权益预估成本、通票凑整参数、渠道费用（通路费与销售渠道费）、卡座提成费率——与高价值判定阈值，每个版本填生效起始日期，
--   批准后待生效，到生效起始日期（按该配置范围的营业日）由系统转正。
-- 依据：BSM:521「所有涉及钱的配置一律版本化：每次改动生成新版本，必须填写生效起始日期，并先存为草案、经高敏权限“转正”审批通过后按指定日期生效，
--   不即改即生效。新版本只对生效日之后发生的业务适用，已发生的业务一律按当时生效的版本计算」；BSM:521「无法判断是否涉及钱的，一律按涉及钱处理。」；
--   ENUM:187「全系统的钱账统一按「营业日」归集。」；第十八批裁决 72（渠道费配置须按版本保存——新版本、生效日、旧版保留）。
-- effective_from 可空：既有生效行为空＝自始有效（研发阶段无存量，UD-07）；新提交的草案必填，须晚于该配置范围当前营业日。
--   六张表里五张与高价值判定阈值表早有该列（V223、V268、V327、V340、V350），赠送权益预估成本表在此补列。
-- status 加 scheduled（已批准、待生效）：到日由系统转正（同范围旧生效行软替换 inactive、本行 active）。各表草案槽与生效槽的部分唯一索引
--   只约束 status = active（V329、V340、V350、V365、V373、V555），待生效行不占槽，索引不动。
ALTER TABLE pass_gift_cost_configs ADD COLUMN effective_from DATE;
COMMENT ON COLUMN pass_gift_cost_configs.effective_from IS
    '生效起始日期（按该配置范围营业日）：批准后待生效（status=scheduled），到日由系统转正；为空＝自始有效（既有行）。';

ALTER TABLE ticket_distribution_configs DROP CONSTRAINT chk_tdc_status;
ALTER TABLE ticket_distribution_configs ADD CONSTRAINT chk_tdc_status CHECK (status IN ('active', 'inactive', 'scheduled'));
ALTER TABLE comprehensive_service_fee_configs DROP CONSTRAINT chk_csfc_status;
ALTER TABLE comprehensive_service_fee_configs ADD CONSTRAINT chk_csfc_status CHECK (status IN ('active', 'inactive', 'scheduled'));
ALTER TABLE ticket_combo_discount_configs DROP CONSTRAINT chk_tcdc_status;
ALTER TABLE ticket_combo_discount_configs ADD CONSTRAINT chk_tcdc_status CHECK (status IN ('active', 'inactive', 'scheduled'));
ALTER TABLE channel_fee_configs DROP CONSTRAINT chk_cfc_status;
ALTER TABLE channel_fee_configs ADD CONSTRAINT chk_cfc_status CHECK (status IN ('active', 'inactive', 'scheduled'));
ALTER TABLE bar_commission_rate_configs DROP CONSTRAINT chk_bcrc_status;
ALTER TABLE bar_commission_rate_configs ADD CONSTRAINT chk_bcrc_status CHECK (status IN ('active', 'inactive', 'scheduled'));
ALTER TABLE pass_gift_cost_configs DROP CONSTRAINT chk_pgc_status;
ALTER TABLE pass_gift_cost_configs ADD CONSTRAINT chk_pgc_status CHECK (status IN ('active', 'inactive', 'scheduled'));
ALTER TABLE high_value_thresholds DROP CONSTRAINT chk_hvt_status;
ALTER TABLE high_value_thresholds ADD CONSTRAINT chk_hvt_status CHECK (status IN ('active', 'inactive', 'scheduled'));
