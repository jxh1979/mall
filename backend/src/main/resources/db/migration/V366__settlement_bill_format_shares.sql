-- V366 通票分账瀑布重建 S11(联合通票业态份额拆分):结算单 × 业态 份额留痕表。
--
-- 【业务依据】docs/business-system-map-for-operations-v0.1.html:742「综合／联合通票收入分账」逐字:
--     「先扣掉第三方渠道抽成(整单扣、在拆份额之前);剩余按各业态票面原价的占比拆成各自份额——
--       游乐份额归游乐公司、餐饮份额归餐厅商户、零售份额归零售商户;酒吧份额再依次扣掉赠送权益预估成本、
--       门票综合服务费,剩余进入可分配池由各酒吧分配。拆份额只按业态大分类进行……
--       每个权益同时带票面原价与内部结算价:前者用于定价与拆份额,后者用于与提供商户结算成本,两者不可混用。」
--   同 :741「定价构成」:售价 = 各业态价格相加后统一打折;免费的(价格为 0)不计入价值。
--   同 :737「免费权益的成本承担」:商户让利型与平台出资型「两类都因价格为 0 而不参与份额拆分」。
--   业态词表(同 :1141):merchants.merchant_type 五值(酒吧/餐饮/零售/游乐/其他),V348 已 CHECK 封口。
--
-- 【为什么要这张表(业务人员角度)】
--   一张联合通票的钱现在只在结算单上显示成一个总数,财务对账时看不出「这个月游乐分走了多少、
--   餐厅分走了多少、真正进酒吧池的又是多少」——对不上就只能翻逐笔核销流水人工加。
--   把每个业态的毛额/折扣补回/渠道费/净额各记一行,对账页一眼能看完,出错时也能指着某一行说是哪一段错了。
--
-- 【为什么不做成 settlement_bills 上的 format_share_* 列】
--   五个业态 × 四个金额 = 20 列,再加业态就要再动一次表结构;做成明细行,加业态只是多一行数据。
--
-- 【金额带符号】跨月红冲(原月已过账的核销被冲正)会让某业态当月份额为负 = 向该业态追回,
--   故不加 >= 0 的 CHECK(照 V335 放开 combo_discount_cent 的先例)。
--
-- 【零金额影响】本迁移只建留痕表,不改任何既有列/既有金额。开发环境数据最后全删,不做存量回填。

CREATE TABLE IF NOT EXISTS settlement_bill_format_shares (
    bill_id                      VARCHAR(64) NOT NULL REFERENCES settlement_bills(id) ON DELETE CASCADE,
    business_format              VARCHAR(16) NOT NULL,   -- bar/catering/retail/amusement/other(V348 五值)
    gross_revenue_cent           BIGINT      NOT NULL DEFAULT 0,   -- 该业态票面份额毛额(已核销净额 + 项目承担的折扣补回)
    platform_borne_discount_cent BIGINT      NOT NULL DEFAULT 0,   -- 其中项目公司承担的套票折扣(§20.4 口径扩到所有业态)
    channel_fee_cent             BIGINT      NOT NULL DEFAULT 0,   -- 整单渠道抽成按份额占比分摊到本业态的部分
    net_revenue_cent             BIGINT      NOT NULL DEFAULT 0,   -- 净份额 = 毛额 − 渠道费(酒吧业态的净份额即可分配池基数)
    beneficiary_kind             VARCHAR(16) NOT NULL,   -- entity=归某经营主体(酒吧→娱乐平台公司/游乐→游乐公司);per_merchant=归提供该权益的各商户自己
    beneficiary_entity_id        VARCHAR(64),            -- beneficiary_kind=entity 时的 business_entities.id;归属未配置时留空(仅留白,不改金额)
    created_at                   TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at                   TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT pk_sbfs PRIMARY KEY (bill_id, business_format),
    CONSTRAINT chk_sbfs_format CHECK (business_format IN ('bar', 'catering', 'retail', 'amusement', 'other')),
    CONSTRAINT chk_sbfs_beneficiary CHECK (
           (beneficiary_kind = 'entity')
        OR (beneficiary_kind = 'per_merchant' AND beneficiary_entity_id IS NULL))
);

COMMENT ON TABLE settlement_bill_format_shares IS
    'S11 结算单 × 业态份额留痕:整单先扣渠道抽成、剩余按各业态票面原价占比拆出的份额。酒吧份额进可分配池,其余业态各走自己的结算出口。';

-- 按业态跨月看某个业态分了多少(经营分析/对账筛选)。
CREATE INDEX IF NOT EXISTS idx_sbfs_format ON settlement_bill_format_shares (business_format, bill_id);

-- 新表必发 GRANT(V296/V298/V338 漏过 → 运行时静默 permission denied 500)。
DO $$
BEGIN
    IF EXISTS (SELECT 1 FROM pg_roles WHERE rolname = 'complex_app') THEN
        EXECUTE 'GRANT SELECT, INSERT, UPDATE, DELETE ON settlement_bill_format_shares TO complex_app';
    END IF;
END $$;
