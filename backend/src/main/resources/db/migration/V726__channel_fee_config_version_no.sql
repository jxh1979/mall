-- 文件版本：V0.01
-- V726（第十八批 BQ 段第 360 条 360-R1、360-R2）：渠道费配置按版本保存，每单记录所用版本号。
-- 依据：第十八批裁决 72「渠道费配置须按版本保存（新版本、生效日、旧版保留），每单记录所用版本号」；
--   ENUM:51「每笔分摊的结果与所用合同版本必须留痕」；BSM:749「每笔分摊须记录所用合同版本」；
--   BSM:521「每一笔单据在生成时就把所用配置版本固化在单上」。
-- 生效起始日期、批准后待生效与到日转正见 V725（371-R1），本迁移只加版本号。
-- version_no：复核批准时按（项目、费用层、渠道）范围取同范围非草案行最大号加一（草案可反复覆盖，不算版本），旧版行留库；
--   既有行即第 1 版（研发阶段无存量，UD-07，不回填）。
ALTER TABLE channel_fee_configs ADD COLUMN version_no INTEGER NOT NULL DEFAULT 1;
COMMENT ON COLUMN channel_fee_configs.version_no IS
    '渠道费配置版本号：复核批准时按（项目、费用层、渠道）范围递增，旧版行留库（第十八批裁决 72）；既有行为第 1 版。';

ALTER TABLE ticket_order_channel_fees ADD COLUMN sales_fee_config_version INTEGER;
COMMENT ON COLUMN ticket_order_channel_fees.sales_fee_config_version IS
    '付款时所用销售渠道费配置的版本号（第十八批裁决 72「每单记录所用版本号」）；官方渠道与只记通路费分摊进度的行为空。';

ALTER TABLE ticket_payment_route_fees ADD COLUMN config_version INTEGER;
COMMENT ON COLUMN ticket_payment_route_fees.config_version IS
    '付款时所用通路费配置的版本号（第十八批裁决 72）；储值笔与未配置通路为空。';
