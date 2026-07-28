-- V348 商户业态标准字段（通票分账重建硬前置）
-- 真源：docs/business-system-map-for-operations-v0.1.html §商户管理 开篇 + docs/business-enum-quick-reference-v0.1.html「商户业态」行：
--   「每个商户必须归入一个业态：酒吧、餐饮、零售、游乐、其他。业态是通票分账按业态拆份额与分流结算的依据，
--     招商录入商户时必填。业态主体「娱乐平台公司」对应的即酒吧业态，不另设「娱乐」业态。」
-- 落法：复用既有 merchants.merchant_type（已 NOT NULL，V267「酒吧＝merchant_type=bar 的商户」已按此口径用它），
--   把它定为权威业态字段 → 五值 CHECK 封口；不新建字段/新建表，避免同一语义两个字段各写各的。
-- 业务上：业态错＝分账份额拆错、钱分错人。故词表由数据库封口，谁也绕不过去（后台/招商/脚本/直连 SQL 一视同仁）。
-- .160 为开发环境，库内数据最后全删 → 不做存量兼容/退化设计，旧词表一次性归一即可。

-- ① 旧词表（tenant/service/entertainment 等）不在五值内 → 一次性归一到「其他」，让 CHECK 能加上。
UPDATE merchants
   SET merchant_type = 'other', updated_at = now()
 WHERE merchant_type NOT IN ('bar', 'catering', 'retail', 'amusement', 'other');

-- ② 业态五值封口。merchant_type 本就 NOT NULL → NOT NULL + CHECK 即「必须归入一个业态」。
ALTER TABLE merchants
    ADD CONSTRAINT chk_merchants_business_format
    CHECK (merchant_type IN ('bar', 'catering', 'retail', 'amusement', 'other'));

-- ③ 按业态查询（分账按业态拆份额、招商/财务按业态筛商户）。
CREATE INDEX IF NOT EXISTS idx_merchants_business_format ON merchants (merchant_type);

-- ④ 招商线索的「意向业态」签约时直接带入自动建档商户的业态（LeasingService.autoCreateMerchantForSign），
--    词表不一致会让招商签约卡在 ② 的 CHECK 上 → 同一词表同步封口（线索阶段可以还没定，故允许为空）。
UPDATE leasing_leads
   SET intended_business_type = 'other', updated_at = now()
 WHERE intended_business_type IS NOT NULL
   AND intended_business_type NOT IN ('bar', 'catering', 'retail', 'amusement', 'other');

ALTER TABLE leasing_leads
    ADD CONSTRAINT chk_leasing_leads_business_format
    CHECK (intended_business_type IS NULL
           OR intended_business_type IN ('bar', 'catering', 'retail', 'amusement', 'other'));
