-- V381 渠道抽成补「固定金额」模式 —— 对齐业务 HTML :739/:742「先扣掉第三方渠道抽成(比例/金额后台可配)」。
--   现 channel_fee_configs 仅 fee_bps(比例);加 fixed_amount_cent(固定金额,分,语义=每张票收固定金额,
--   与 V328 门票综合服务费固定额同语义——渠道按票收技术服务费,一张票收一道,冲正取反)。
--   二选一范式照 V328 chk_csfc_fee_mode:配了固定金额则 fee_bps 必须 0(CHECK 保证不同时既比例又固定金额)。
--   ⚠️ 仅加可空列 + CHECK;现存行 fixed_amount_cent=NULL=仍走比例(种子 fee_bps=0=不抽),迁移前后分账生效值不变,可独立回滚。
ALTER TABLE channel_fee_configs
    ADD COLUMN IF NOT EXISTS fixed_amount_cent BIGINT;

ALTER TABLE channel_fee_configs
    ADD CONSTRAINT chk_cfc_fee_mode
    CHECK (fixed_amount_cent IS NULL OR (fee_bps = 0 AND fixed_amount_cent >= 0));
