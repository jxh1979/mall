-- 文件版本：V0.01
-- M021-001：V384 把商户业态词表放开到六值（加 media=演出传媒）时，只同步了 merchants 与 leasing_leads 两处 CHECK，
--   漏了同词表的第三处 settlement_bill_format_shares.chk_sbfs_format（V366 建表时为 V348 五值）。
--   月结的业态取自 merchants.merchant_type（JdbcSettlementBillRepository.aggregateFormatRevenue），
--   media 业态商户的权益进联合通票月结时，份额留痕行写不进去，整张结算单生成失败。
-- 业务依据（最终解释权=业务 HTML，逐字）：
--   business-enum-quick-reference「商户业态」：「酒吧、餐饮、零售、游乐、演出传媒（系统枚举码 media）、其他。」
--   business-system-map-for-operations「收入归属」：「演出票 → 传媒公司」。
-- 已应用的 V366/V384 不改写；本迁移只把这一处 CHECK 放开到同一六值闭集，不改任何金额列与既有行。
ALTER TABLE settlement_bill_format_shares DROP CONSTRAINT IF EXISTS chk_sbfs_format;
ALTER TABLE settlement_bill_format_shares
    ADD CONSTRAINT chk_sbfs_format
    CHECK (business_format IN ('bar', 'catering', 'retail', 'amusement', 'media', 'other'));
