-- 第二阶段（待确认#4 遗留）：为合同准入闸 over_contract_scope / over_space 真实判定补数据模型。
--
-- 背景：AdmissionGuard 的 over_contract_scope / over_space 此前因缺字段保守放行（见类注释 TODO）。
--   contracts（V010）有 merchant_id/effective_date/expiry_date/lifecycle_status，但无"经营范围"列；
--   spaces（V005）只到 merchant_id，无 contract_id；object→space 亦无通用映射。
--
-- 本迁移补两块：
--   1) contracts.allowed_business_scope JSONB：合同允许的业务域列表，如 ["bar","amusement","ticketing"]。
--      取 actionType 第一段为业务域（bar.bill_waiver→bar / amusement.reopen→amusement /
--      ticketing.special_release→ticketing / finance.*→finance）。
--      口径：scope 不含该业务域 → over_contract_scope 命中；scope 为空 [] → 视为"未配置=不限"放行
--      （保守不误伤，可后台收紧为白名单语义）。无生效合同亦命中（与 expired 语义一致）。
--   2) contract_spaces(contract_id, space_id) 关联表：合同租赁空间映射（多对多，合同可租多空间）。
--      选关联表而非 spaces.contract_id：一空间可被不同时段/合同复用，且合同可含多空间，关联表更贴合。
--      over_space：object→space 解析出空间后，该空间不在 actor merchant 生效合同的 contract_spaces 内 → 命中。

-- 1) 合同经营范围
ALTER TABLE contracts
    ADD COLUMN IF NOT EXISTS allowed_business_scope JSONB NOT NULL DEFAULT '[]'::jsonb;

COMMENT ON COLUMN contracts.allowed_business_scope IS
    '合同允许的业务域列表（JSON 数组，元素如 bar/amusement/ticketing/finance）。空数组=未配置不限。';

-- 2) 合同-空间 租赁关联表
CREATE TABLE IF NOT EXISTS contract_spaces (
    contract_id VARCHAR(64) NOT NULL REFERENCES contracts(id),
    space_id    VARCHAR(64) NOT NULL REFERENCES spaces(id),
    created_at  TIMESTAMPTZ NOT NULL DEFAULT now(),
    PRIMARY KEY (contract_id, space_id)
);

CREATE INDEX IF NOT EXISTS idx_contract_spaces_space
    ON contract_spaces (space_id);
