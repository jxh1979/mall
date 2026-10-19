-- 文件版本：V0.01
-- V511：FS-024 商户档案「主管公司」与「理由」（仅业态为其他时必填）。
-- 依据：ENUM L195「选“其他”的商户，必须在商户档案里指定一家主管公司并说明理由，未指定的不得开通任何结算与分账」；
--   操作员裁决 2026-10-19 第二轮第 7 条「本批补字段与闸」；控制席补充：主管公司只能从在用的域主管公司／城市项目公司
--   （business_entities 在用主体）中选（角色与在用状态由应用层写口校验）。
-- 其他业态不得填写：由 CHECK 封口。不为存量「其他」业态商户回填：未指定的由结算/分账写口拒绝、到期作业单行记错。
ALTER TABLE merchants
    ADD COLUMN supervising_company_id     VARCHAR(64) REFERENCES business_entities(id),
    ADD COLUMN supervising_company_reason TEXT,
    ADD CONSTRAINT chk_merchants_supervising_company_other_only
        CHECK (merchant_type = 'other' OR (supervising_company_id IS NULL AND supervising_company_reason IS NULL));
