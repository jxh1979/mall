-- V328 结算配置中心批1(通票分账)-2:门票综合服务费补「固定额」选项 —— 对齐 PRD §7.2 口径8「按比例 or 固定额」。
--   现 comprehensive_service_fee_configs 仅 fee_bps(比例);加 fixed_amount_cent(固定额,分)。
--   二选一语义:配了固定额则 fee_bps 必须 0(CHECK 保证不同时既比例又固定额)。
--   ⚠️ 保值:仅加可空列 + CHECK;现存行 fixed_amount_cent=NULL=仍走比例(默认 fee_bps=0=不抽),迁移前后分账生效值不变,可独立回滚。
ALTER TABLE comprehensive_service_fee_configs
    ADD COLUMN IF NOT EXISTS fixed_amount_cent BIGINT;

ALTER TABLE comprehensive_service_fee_configs
    ADD CONSTRAINT chk_csfc_fee_mode
    CHECK (fixed_amount_cent IS NULL OR (fee_bps = 0 AND fixed_amount_cent >= 0));
