-- V225 结算重构批3-1(权益结算细则 PRD §6.1):实物/虚拟权益结算价字段。
--   口径:实物权益(酒水等)按**内部结算价**(bar_products.internal_settlement_price_cent,V152)结算;
--         虚拟权益(入场资格/服务)按**签约权益单价**(agreement_price_cent)结算。
--   本迁移:①entitlement_templates/issued_entitlements 加 entitlement_kind + agreement_price_cent;
--          ②ticket_verifications 加 settlement_price_cent(核销时记该次结算价,供对账/by_entitlement_price 分账);
--          ③回填现有权益 kind(按 entitlement_type:酒水类=实物,其余=虚拟)。
--   核销时"实物取内部价/虚拟取签约价/回退 value_amount"的判定逻辑在配套代码(批3-1)。
--   仅加列+回填,不改现有逻辑,可独立回滚。字段允许 NULL(向后兼容,判定逻辑对 NULL 回退 value_amount)。

-- ① 权益模板 + 实例:kind(physical/virtual) + agreement_price_cent(虚拟签约单价,可空)
ALTER TABLE entitlement_templates
    ADD COLUMN IF NOT EXISTS entitlement_kind      VARCHAR(16),
    ADD COLUMN IF NOT EXISTS agreement_price_cent  BIGINT;

ALTER TABLE issued_entitlements
    ADD COLUMN IF NOT EXISTS entitlement_kind      VARCHAR(16),
    ADD COLUMN IF NOT EXISTS agreement_price_cent  BIGINT;

-- ② 核销记录加结算价(核销时判定后写入,供对账与 by_entitlement_price 分账)
ALTER TABLE ticket_verifications
    ADD COLUMN IF NOT EXISTS settlement_price_cent BIGINT;

-- ③ 回填现有权益 kind:酒水类(gift_drink)=实物,走内部结算价;其余(入场/游乐/服务)=虚拟,走签约单价。
--    运营后台可后续在权益模板编辑 kind/签约单价;此处保守回填,新发放应显式带 kind。
UPDATE entitlement_templates
   SET entitlement_kind = CASE WHEN entitlement_type IN ('gift_drink') THEN 'physical' ELSE 'virtual' END
 WHERE entitlement_kind IS NULL;

UPDATE issued_entitlements
   SET entitlement_kind = CASE WHEN entitlement_type IN ('gift_drink') THEN 'physical' ELSE 'virtual' END
 WHERE entitlement_kind IS NULL;

-- ④ 取值约束(回填后加;允许 NULL 向后兼容,判定逻辑对 NULL 回退 value_amount_cent)
ALTER TABLE entitlement_templates
    ADD CONSTRAINT chk_et_kind CHECK (entitlement_kind IS NULL OR entitlement_kind IN ('physical','virtual'));
ALTER TABLE issued_entitlements
    ADD CONSTRAINT chk_ie_kind CHECK (entitlement_kind IS NULL OR entitlement_kind IN ('physical','virtual'));
