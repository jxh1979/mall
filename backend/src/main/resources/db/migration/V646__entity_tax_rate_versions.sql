-- 文件版本：V0.01
-- V646：第十八批 AR 段（batch64）第 233 条税务处理 B 段：233-R2 税率按主体 × 收入类型 × 税种在后台版本化维护，走财税法审核加最高确认。
-- 依据：ENUM:201「每个主体 × 每类收入 × 每种税（增值税/文化事业建设费/附加税）后台各自配“有没有这笔费用 + 收多少”」「税率、税目不属于普通后台
--   配置项」；BSM:1760「企业所得税等税种：系统内配置税率比例」「所有税费规则和税费调整均须经过财务审核、税务审核、法务审核，再由顶层平台公司的老板最终
--   确认后生效」；BSM:521「所有涉及钱的配置一律版本化：每次改动生成新版本，必须填写生效起始日期，并先存为草案、经高敏权限“转正”审批通过后按指定日期
--   生效」「新版本只对生效日之后发生的业务适用，已发生的业务一律按当时生效的版本计算」。
-- 1) 作用域由项目改为经营主体：business_entity_id 为空＝平台默认。project_id 全仓只有开票清单取税率读过、种子全为空，研发阶段无存量（UD-07），删去。
--    V223 种子行不动（平台默认，effective_from 为空＝自始有效）。
-- 2) 版本留痕：reason、review_id（finance.tax_rule.adjust 复核链，EntityTaxService）、evidence_attachment_ids、created_by。状态沿用 V223 两列：
--    待审 is_draft=TRUE、status=active；已生效 is_draft=FALSE、status=active；驳回 status=inactive（草案留痕）。
-- 3) 税种加 corporate_income（企业所得税，收入类型固定 all）；其他税费不配比例（第十八批裁决 59：按季与财税软件核对、以财税软件为准）。
--    income_type 不设 CHECK，闭集在服务层校验（V223 九类＋本条 goods 供货货款、parking 停车费、retail 零售）。
-- 4) 同主体、同收入类型、同税种、同生效日至多一条待审或已生效版本。读取（JdbcSettlementInvoiceRepository.findTaxRate）：主体特例优先，再取生效日
--    不晚于业务日期的最新已生效版本，生效日为空的种子排最后；条件里不滤 applicable（新版本关掉这笔费用时不退回旧版本）。
DROP INDEX IF EXISTS uq_stc;
ALTER TABLE settlement_tax_configs ADD COLUMN IF NOT EXISTS business_entity_id VARCHAR(64);
ALTER TABLE settlement_tax_configs DROP COLUMN IF EXISTS project_id;
ALTER TABLE settlement_tax_configs ADD COLUMN IF NOT EXISTS reason TEXT;
ALTER TABLE settlement_tax_configs ADD COLUMN IF NOT EXISTS review_id VARCHAR(64);
ALTER TABLE settlement_tax_configs ADD COLUMN IF NOT EXISTS evidence_attachment_ids JSONB NOT NULL DEFAULT '[]'::jsonb;
ALTER TABLE settlement_tax_configs ADD COLUMN IF NOT EXISTS created_by VARCHAR(64);
ALTER TABLE settlement_tax_configs DROP CONSTRAINT IF EXISTS chk_stc_tax;
ALTER TABLE settlement_tax_configs ADD CONSTRAINT chk_stc_tax CHECK (tax_type IN ('vat', 'cultural_fee', 'surcharge', 'corporate_income'));
CREATE UNIQUE INDEX IF NOT EXISTS uq_stc_version ON settlement_tax_configs
    (COALESCE(business_entity_id, '__DEFAULT__'), income_type, tax_type, COALESCE(effective_from, DATE '1900-01-01')) WHERE status = 'active';
COMMENT ON TABLE settlement_tax_configs IS '税率版本（ENUM:201 每个主体×每类收入×每种税配有没有这笔费用与比例；BSM:521 版本化、填生效日、审批转正；business_entity_id 为空＝平台默认）';
