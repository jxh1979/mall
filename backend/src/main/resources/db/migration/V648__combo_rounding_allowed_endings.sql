-- 文件版本：V0.01
-- V648（第十八批 AT 段第 292 条 292-R1）：通票凑整允许尾数集合入库，在结算配置页「通票分账」全平台默认里配置，经差人复核转正生效。
-- 依据：BSM:754「运营可在此基础上填一个近似售价用于凑整（使最终售价为整数，且尾数落在后台配置的允许尾数集合内——业务参数，全平台统一可配，
--   出厂默认 8、9、0）」；ENUM:193「最终售价为整数且尾数落在后台配置的允许尾数集合内（业务参数，全平台统一可配，出厂默认 8/9/0）」。
-- 存升序去重的尾数数字串，出厂 8、9、0 记 '089'；取值只认全平台默认行（project_id 为空），票务凑整建议按它算。

ALTER TABLE ticket_combo_discount_configs
    ADD COLUMN rounding_allowed_endings VARCHAR(10) NOT NULL DEFAULT '089',
    ADD CONSTRAINT chk_tcdc_rounding_allowed_endings CHECK (rounding_allowed_endings ~ '^[0-9]{1,10}$');

COMMENT ON COLUMN ticket_combo_discount_configs.rounding_allowed_endings IS '通票凑整允许尾数集合（升序去重的尾数数字串，出厂 089＝8、9、0）：BSM:754「尾数落在后台配置的允许尾数集合内——业务参数，全平台统一可配，出厂默认 8、9、0」；只取全平台默认行';
