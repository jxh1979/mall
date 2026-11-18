-- 文件版本：V0.01
-- V647：第十八批 AR 段（batch64）第 233 条税务处理 C 段：233-R5 增值税、文化事业建设费、附加税生成系统计算金额，按申报差异出补提或冲回调整单。
-- 依据：BSM:1760「税费金额分三个阶段分别保存、互不覆盖：系统计算金额、实际申报金额、实际缴纳金额；申报差异等于实际申报金额减系统计算金额……
--   申报金额高于系统计算金额的生成补提调整单、低于的生成冲回调整单。两套差异机制按税种互斥适用、不得叠加：增值税、文化事业建设费等流转税按申报差异
--   生成补提／冲回调整单，差异保留、不做拉平；企业所得税等税种按季度与财税软件核对拉平（以财税软件为准）」；BSM:521「每一笔单据在生成时就把所用
--   配置版本固化在单上」；第十八批裁决 59「附加税跟流转税走补提或冲回调整单；其他税费和企业所得税一样，按季与财税软件核对、以财税软件为准拉平」。
-- system_calculated_amount_cent：登记申报时按所属期每月、每类收入（233-R4 EntityIncomeService）与当时生效的税率版本（233-R2）现算，随登记落库，
--   之后任何流程都不改；为空表示没有系统计算基数（其他税费，裁决 59）。
-- calculation_basis：逐月、逐收入类型的计算依据 [{month, income_type, income_cent, rate_bps, rate_version_id, tax_amount_cent, …}]，附加税另带
--   vat_amount_cent、vat_rate_bps、vat_rate_version_id（以同期系统计算增值税为基数），企业所得税（233-R7）为一项、另带 profit（所属期收入成本利润）；
--   rate_version_id 即所用 settlement_tax_configs 行 id。
-- 补提、冲回调整单落在 V645 entity_tax_adjustments（kind supplement／reversal），申报批准时同事务生成，不进分红（分红仍扣申报额）。研发阶段无存量（UD-07）。
ALTER TABLE entity_tax_declarations ADD COLUMN IF NOT EXISTS system_calculated_amount_cent BIGINT;
ALTER TABLE entity_tax_declarations ADD COLUMN IF NOT EXISTS calculation_basis JSONB;
COMMENT ON COLUMN entity_tax_declarations.system_calculated_amount_cent IS '系统计算金额（BSM:1760 三阶段之一；登记时按收入归集与当时生效税率现算，不改；空＝没有系统计算基数）';
COMMENT ON COLUMN entity_tax_declarations.calculation_basis IS '系统计算依据：逐月、逐收入类型的收入、比例、所用税率版本 id 与税额（BSM:521 所用配置版本固化在单上）';
