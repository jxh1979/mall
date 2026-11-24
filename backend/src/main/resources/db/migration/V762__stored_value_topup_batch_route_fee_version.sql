-- 文件版本：V0.01
-- V762（第十八批 CB 段第 396 条 396-R1）：通用卡充值批次记下充值支付成功时所用的通路费配置行与版本号。
-- 依据：BSM:521「每一笔单据在生成时就把所用配置版本固化在单上」；第十八批裁决 72「渠道费配置须按版本保存（新版本、生效日、旧版保留），
--   每单记录所用版本号」；ENUM:86「通道费以每笔充值支付成功时平台与该支付通路的生效合同费率为准，随该笔充值一并固化」。
-- 版本号取 channel_fee_configs.version_no（V726）；该支付通路未配置通路费时两列为空、费率为 0（不拒绝入账）。
-- 不回填既有批次（研发阶段无存量，UD-07）。
ALTER TABLE stored_value_topup_batches ADD COLUMN fee_config_id VARCHAR(48);
ALTER TABLE stored_value_topup_batches ADD COLUMN fee_config_version INTEGER;
COMMENT ON COLUMN stored_value_topup_batches.fee_config_id IS
    '充值支付成功时所用通路费配置行（channel_fee_configs.id，BSM:521、第十八批裁决 72）；未配置通路为空。';
COMMENT ON COLUMN stored_value_topup_batches.fee_config_version IS
    '充值支付成功时所用通路费配置的版本号（channel_fee_configs.version_no，第十八批裁决 72「每单记录所用版本号」）；未配置通路为空。';
