-- V318 组织四层落库:business_entities 增 parent_entity_id,把「管理公司/业态主体(L3)」层显式挂到父级。
--   口径来源:terminology-canonical-v1.0.md §一(四层实体)/§三(六类业态主体=管理公司层)。
--   四层:平台(L1) → 项目公司(L2) → 管理公司/业态主体(L3) → 商户(merchants,另表)。
--   L1/L2/L3 均落在 business_entities(entity_role=platform / project_company / management_company/spv/...);
--   本迁移补上「层级 parent 链」:L3.parent=L2 项目公司,L2.parent=L1 平台,L1.parent=NULL。
--   「一个项目公司下有多个管理公司主体」= 多行 business_entities(parent_entity_id=该项目公司)。
--   parent_entity_id 松引用 business_entities(id)(与 merchants.business_entity_id 一致,本库约定不加硬 FK)。
--   business_entities 已在 V005 建表并 GRANT 给 complex_app;ALTER ADD COLUMN 继承既有授权,无需再 GRANT。
ALTER TABLE business_entities
    ADD COLUMN IF NOT EXISTS parent_entity_id VARCHAR(64);

COMMENT ON COLUMN business_entities.parent_entity_id IS
    '四层组织父级(terminology-canonical §一):L3 管理公司→L2 项目公司,L2→L1 平台,L1=NULL';

CREATE INDEX IF NOT EXISTS idx_business_entities_parent
    ON business_entities (parent_entity_id);
