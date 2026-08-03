-- V384 商户业态词表补「传媒」+ 演出票收入归属种子（2026-08-03 口径矫回）。
--
-- 【业务依据】(最终解释权=业务 HTML,逐字)
--   business-enum-quick-reference §「到期未核销款」:「余额按票种经营域归属:酒吧相关归娱乐平台公司,
--     纯游乐归游乐公司,【演出归传媒公司】;跨域组合通票按既定域份额归属。」
--   business-system-map-for-operations §「收入归属:哪类收入归哪个业态主体」:「演出票 → 传媒公司」。
--
-- 【改前的错】V348 把 merchants.merchant_type 封死在 bar/catering/retail/amusement/other 五值,
--   压根没有传媒业态 → 演出提供方只能录成「其他」→ 结算按业态分流时落不到 performance_ticket 流,
--   演出票的到期未核销款必然错归娱乐平台公司。钱进错主体的账,传媒公司月底根本收不到,
--   而账面看着完全正常(娱乐平台公司那一栏只是「多了一点」)。
--
-- .160 为开发环境、库内数据最后全删 → 不做存量兼容/回填设计,词表一次性放开即可。

-- ① 业态六值封口(加 media=传媒)。业态错＝分账份额拆错、钱分错人,故词表仍由数据库封口。
ALTER TABLE merchants DROP CONSTRAINT IF EXISTS chk_merchants_business_format;
ALTER TABLE merchants
    ADD CONSTRAINT chk_merchants_business_format
    CHECK (merchant_type IN ('bar', 'catering', 'retail', 'amusement', 'media', 'other'));

-- ② 招商线索「意向业态」同词表同步(V348 ④ 同因:词表不一致会让招商签约卡在 ① 的 CHECK 上)。
ALTER TABLE leasing_leads DROP CONSTRAINT IF EXISTS chk_leasing_leads_business_format;
ALTER TABLE leasing_leads
    ADD CONSTRAINT chk_leasing_leads_business_format
    CHECK (intended_business_type IS NULL
           OR intended_business_type IN ('bar', 'catering', 'retail', 'amusement', 'media', 'other'));

-- ③ 演出票收入归属种子(照 V375 范式种全局默认行,项目级仍可在结算配置中心覆盖,零硬编码)。
--    主体 be_media「传媒公司」由 V354 建档。没有这一行,演出提供方的到期未核销款会 fail-loud 拒绝落账
--    (有金额没有收款方=配置坏,宁可阻断不出错账)。
INSERT INTO revenue_stream_attribution
    (id, project_id, revenue_stream, management_entity_id, beneficiary_kind, status, is_draft, updated_by, updated_at)
SELECT 'rsa_performance_ticket_default', NULL, 'performance_ticket', 'be_media', 'entity',
       'active', FALSE, 'migration:V384', now()
 WHERE NOT EXISTS (SELECT 1 FROM revenue_stream_attribution
                    WHERE revenue_stream = 'performance_ticket'
                      AND project_id IS NULL AND status = 'active' AND is_draft = FALSE);
