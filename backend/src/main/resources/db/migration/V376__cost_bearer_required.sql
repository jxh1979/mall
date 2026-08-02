-- V376 赠送权益成本承担方改必填(2026-08-01 用户拍板:漏标拒发布,不再默认平台出资型)。
--
-- 【业务依据】business-system-map-for-operations §「免费权益的成本承担」(:737):
--   「每个权益必须在票种配置里标明成本承担方」——V372 曾把缺省落成 platform_funded(平台出资型),
--   运营漏标一个商户让利型赠品,平台就替商户多付一笔预估成本。现改为:
--   NULL = 还没标 → 赠送类权益(票面原价 0 且提供方兑付类)保存/发布一律拒绝(写入面校验);
--   非赠送类权益(入场/付费组成等)无成本承担概念,允许留空。
--
-- 开发环境,存量数据最后全删,不做回填/兼容。

ALTER TABLE ticket_type_entitlements ALTER COLUMN cost_bearer DROP DEFAULT;
ALTER TABLE ticket_type_entitlements ALTER COLUMN cost_bearer DROP NOT NULL;

-- 值域 CHECK 放开 NULL(NULL=未标,由写入面/发布闸拦;坏值仍拒)。
ALTER TABLE ticket_type_entitlements DROP CONSTRAINT IF EXISTS chk_tte_cost_bearer;
ALTER TABLE ticket_type_entitlements ADD CONSTRAINT chk_tte_cost_bearer
    CHECK (cost_bearer IS NULL OR cost_bearer IN ('platform_funded', 'merchant_concession'));

COMMENT ON COLUMN ticket_type_entitlements.cost_bearer IS
    'S6/V376 免费·赠送权益的成本承担方(HTML:737「每个权益必须在票种配置里标明」,必填无默认):platform_funded=平台出资型(进 G 预扣·核销按内部结算价与提供商户结算);merchant_concession=商户让利型(平台不承担成本·不与商户结算·不进 G);NULL=未标——赠送类权益(票面原价 0 且提供方兑付类)未标则拒保存/拒发布,非赠送类无此概念可留空';
