-- 结算重构批2:综合服务费「月度封顶」(可配·默认不封顶·比例与固定额两模式共用)。
-- 口径(用户 2026-07-21 拍板):封顶配在每条综合服务费费率规则上(项目×商户×票种作用域),
--   该规则当月产生的服务费不超过封顶值 monthly_cap_amount_cent(绝对分值)。
-- 引擎每月每 (商户×票种) 聚合行算出 fee 后 clamp:fee = min(fee, monthly_cap_amount_cent)。
-- 默认 NULL = 不封顶 → 零行为变更(与现有默认费率 0 一致的保值语义)。
-- 两模式共用一个绝对上限列:比例(fee_bps)与固定额(fixed_amount_cent)算出的 fee 都受同一封顶约束。
ALTER TABLE comprehensive_service_fee_configs
    ADD COLUMN IF NOT EXISTS monthly_cap_amount_cent BIGINT;

ALTER TABLE comprehensive_service_fee_configs
    ADD CONSTRAINT chk_csfc_monthly_cap
    CHECK (monthly_cap_amount_cent IS NULL OR monthly_cap_amount_cent >= 0);
